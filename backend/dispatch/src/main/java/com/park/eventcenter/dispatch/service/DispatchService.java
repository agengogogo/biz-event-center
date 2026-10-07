package com.park.eventcenter.dispatch.service;

import com.park.eventcenter.common.error.NotFoundException;
import com.park.eventcenter.common.idempotency.IdempotencyStore;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.dispatch.delivery.DeliveryRequest;
import com.park.eventcenter.dispatch.delivery.DeliveryTarget;
import com.park.eventcenter.dispatch.delivery.DeliveryTargetRegistry;
import com.park.eventcenter.dispatch.dto.DeliveryView;
import com.park.eventcenter.dispatch.dto.DispatchResult;
import com.park.eventcenter.dispatch.entity.DeliveryEntity;
import com.park.eventcenter.dispatch.repository.DeliveryRepository;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventStatus;
import com.park.eventcenter.model.TraceStage;
import com.park.eventcenter.notification.channel.DeliveryResult;
import com.park.eventcenter.storequery.service.AuditService;
import com.park.eventcenter.storequery.service.EventStoreService;
import com.park.eventcenter.storequery.service.EventTraceService;
import com.park.eventcenter.subscription.entity.SubscriptionEntity;
import com.park.eventcenter.subscription.service.FilterExpressionEvaluator;
import com.park.eventcenter.subscription.service.PayloadTransformer;
import com.park.eventcenter.subscription.service.SubscriptionService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Routes canonical events to matching subscriptions with retry, dead-letter and idempotency. */
@Service
public class DispatchService {

  private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(7);
  private static final long MAX_BACKOFF_MILLIS = 30_000L;

  private final SubscriptionService subscriptionService;
  private final FilterExpressionEvaluator filterEvaluator;
  private final PayloadTransformer payloadTransformer;
  private final DeliveryTargetRegistry targetRegistry;
  private final IdempotencyStore idempotencyStore;
  private final DeliveryRepository deliveryRepository;
  private final EventStoreService eventStoreService;
  private final EventTraceService traceService;
  private final AuditService auditService;

  public DispatchService(
      SubscriptionService subscriptionService,
      FilterExpressionEvaluator filterEvaluator,
      PayloadTransformer payloadTransformer,
      DeliveryTargetRegistry targetRegistry,
      IdempotencyStore idempotencyStore,
      DeliveryRepository deliveryRepository,
      EventStoreService eventStoreService,
      EventTraceService traceService,
      AuditService auditService) {
    this.subscriptionService = subscriptionService;
    this.filterEvaluator = filterEvaluator;
    this.payloadTransformer = payloadTransformer;
    this.targetRegistry = targetRegistry;
    this.idempotencyStore = idempotencyStore;
    this.deliveryRepository = deliveryRepository;
    this.eventStoreService = eventStoreService;
    this.traceService = traceService;
    this.auditService = auditService;
  }

  public DispatchResult dispatch(CanonicalEvent event) {
    List<SubscriptionEntity> subscriptions =
        subscriptionService.findMatching(event.parkId(), event.eventType());
    traceService.append(
        event.eventId(),
        event.parkId(),
        TraceStage.ROUTED,
        "MATCHED",
        "matched=" + subscriptions.size());

    int matched = 0;
    int delivered = 0;
    int failed = 0;
    int deadLettered = 0;
    int skipped = 0;
    for (SubscriptionEntity subscription : subscriptions) {
      if (!filterMatches(subscription, event)) {
        continue;
      }
      matched++;
      CanonicalEvent payloadEvent =
          payloadTransformer.apply(
              event, payloadTransformer.parse(subscription.getTransformRules()));
      DeliveryOutcome outcome = deliver(subscription, payloadEvent);
      switch (outcome) {
        case DELIVERED -> delivered++;
        case DEAD_LETTER, FAILED -> {
          deadLettered += outcome == DeliveryOutcome.DEAD_LETTER ? 1 : 0;
          failed++;
        }
        case SKIPPED -> skipped++;
      }
    }
    eventStoreService.updateStatus(
        event.eventId(),
        event.parkId(),
        deadLettered > 0 ? EventStatus.FAILED : EventStatus.DISPATCHED);
    auditService.record(
        "system",
        "DISPATCH",
        "EVENT",
        event.eventId(),
        event.parkId(),
        "matched=%d delivered=%d deadLettered=%d".formatted(matched, delivered, deadLettered));
    return new DispatchResult(event.eventId(), matched, delivered, failed, deadLettered, skipped);
  }

  public DispatchResult dispatchEvent(String eventId) {
    String parkId = ParkContext.require();
    CanonicalEvent event =
        eventStoreService
            .findCanonical(eventId, parkId)
            .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    traceService.append(eventId, parkId, TraceStage.PUBLISHED, "MANUAL", "manual dispatch");
    return dispatch(event);
  }

  public DispatchResult redeliver(String eventId) {
    String parkId = ParkContext.require();
    return redeliver(eventId, parkId);
  }

  public DispatchResult redeliver(String eventId, String parkId) {
    CanonicalEvent event =
        eventStoreService
            .findCanonical(eventId, parkId)
            .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    traceService.append(eventId, parkId, TraceStage.REDELIVERED, "REQUESTED", "manual redelivery");
    return dispatch(event);
  }

  private boolean filterMatches(SubscriptionEntity subscription, CanonicalEvent event) {
    try {
      return filterEvaluator.matches(subscription.getFilterExpression(), event);
    } catch (RuntimeException ex) {
      traceService.append(
          event.eventId(),
          event.parkId(),
          TraceStage.ROUTED,
          "FILTER_ERROR",
          "subscription=" + subscription.getId() + " error=" + ex.getMessage());
      return false;
    }
  }

  private DeliveryOutcome deliver(SubscriptionEntity subscription, CanonicalEvent event) {
    String deliveredKey = "dispatch:delivered:" + subscription.getId() + ":" + event.eventId();
    if (idempotencyStore.exists(deliveredKey)) {
      return DeliveryOutcome.SKIPPED;
    }
    Optional<DeliveryTarget> target = targetRegistry.find(subscription.getTargetType());
    DeliveryEntity record =
        deliveryRepository
            .findBySubscriptionIdAndEventId(subscription.getId(), event.eventId())
            .orElseGet(
                () -> {
                  DeliveryEntity created = new DeliveryEntity();
                  created.setParkId(event.parkId());
                  created.setEventId(event.eventId());
                  created.setSubscriptionId(subscription.getId());
                  created.setCreatedAt(Instant.now());
                  return created;
                });
    record.setTargetType(subscription.getTargetType());
    record.setTarget(resolveTargetLabel(subscription));
    record.setUpdatedAt(Instant.now());

    if (target.isEmpty()) {
      record.setStatus("FAILED");
      record.setDeadLetter(true);
      record.setLastError("Unsupported target type: " + subscription.getTargetType());
      deliveryRepository.save(record);
      traceService.append(
          event.eventId(),
          event.parkId(),
          TraceStage.DEAD_LETTERED,
          "UNSUPPORTED_TARGET",
          record.getLastError());
      return DeliveryOutcome.DEAD_LETTER;
    }

    DeliveryRequest request =
        new DeliveryRequest(
            event,
            subscription.getId(),
            subscription.getTargetType(),
            subscription.getTargetUrl(),
            subscription.getNotificationChannelId(),
            subscription.getTemplateId());

    int maxRetries = Math.max(subscription.getMaxRetries(), 0);
    String lastError = null;
    int attempt = 0;
    while (attempt <= maxRetries) {
      attempt++;
      DeliveryResult result = target.get().deliver(request);
      record.setAttempts(attempt);
      if (result.success()) {
        record.setStatus("DELIVERED");
        record.setLastError(null);
        record.setDeadLetter(false);
        record.setUpdatedAt(Instant.now());
        deliveryRepository.save(record);
        idempotencyStore.mark(deliveredKey, IDEMPOTENCY_TTL);
        traceService.append(
            event.eventId(),
            event.parkId(),
            TraceStage.DELIVERED,
            "SUCCESS",
            "subscription=" + subscription.getId());
        return DeliveryOutcome.DELIVERED;
      }
      lastError = result.error();
      if (attempt <= maxRetries) {
        traceService.append(
            event.eventId(),
            event.parkId(),
            TraceStage.RETRIED,
            "ATTEMPT_" + attempt,
            "subscription=" + subscription.getId() + " error=" + lastError);
        sleep(backoffMillis(subscription, attempt));
      }
    }

    record.setStatus("DEAD_LETTER");
    record.setDeadLetter(true);
    record.setLastError(lastError);
    record.setUpdatedAt(Instant.now());
    deliveryRepository.save(record);
    traceService.append(
        event.eventId(),
        event.parkId(),
        TraceStage.DEAD_LETTERED,
        "MAX_RETRIES",
        "subscription=" + subscription.getId() + " error=" + lastError);
    return DeliveryOutcome.DEAD_LETTER;
  }

  private long backoffMillis(SubscriptionEntity subscription, int attempt) {
    long base = Math.max(subscription.getBackoffMillis(), 0L);
    long delay = base * (1L << Math.min(attempt - 1, 10));
    return Math.min(delay, MAX_BACKOFF_MILLIS);
  }

  private void sleep(long millis) {
    if (millis <= 0) {
      return;
    }
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private String resolveTargetLabel(SubscriptionEntity subscription) {
    if (subscription.getTargetUrl() != null) {
      return subscription.getTargetUrl();
    }
    return subscription.getNotificationChannelId();
  }

  public List<DeliveryView> list() {
    return deliveryRepository.findByParkIdOrderByCreatedAtDesc(ParkContext.require()).stream()
        .map(DispatchService::toView)
        .toList();
  }

  public List<DeliveryView> listDeadLetters() {
    return deliveryRepository
        .findByParkIdAndDeadLetterTrueOrderByCreatedAtDesc(ParkContext.require())
        .stream()
        .map(DispatchService::toView)
        .toList();
  }

  public DispatchResult redeliverDelivery(Long deliveryId) {
    String parkId = ParkContext.require();
    DeliveryEntity delivery =
        deliveryRepository
            .findByIdAndParkId(deliveryId, parkId)
            .orElseThrow(() -> new NotFoundException("Delivery not found: " + deliveryId));
    return redeliver(delivery.getEventId(), parkId);
  }

  static DeliveryView toView(DeliveryEntity entity) {
    return new DeliveryView(
        entity.getId(),
        entity.getParkId(),
        entity.getEventId(),
        entity.getSubscriptionId(),
        entity.getTargetType(),
        entity.getTarget(),
        entity.getStatus(),
        entity.getAttempts(),
        entity.getLastError(),
        entity.isDeadLetter(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
