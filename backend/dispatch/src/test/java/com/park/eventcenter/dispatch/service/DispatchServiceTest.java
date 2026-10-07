package com.park.eventcenter.dispatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.idempotency.IdempotencyStore;
import com.park.eventcenter.dispatch.delivery.DeliveryRequest;
import com.park.eventcenter.dispatch.delivery.DeliveryTarget;
import com.park.eventcenter.dispatch.delivery.DeliveryTargetRegistry;
import com.park.eventcenter.dispatch.dto.DispatchResult;
import com.park.eventcenter.dispatch.repository.DeliveryRepository;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies routing, filtering, retry/dead-letter and idempotent delivery behaviour. */
class DispatchServiceTest {

  private final RecordingTarget target = new RecordingTarget();
  private SubscriptionService subscriptionService;
  private DeliveryRepository deliveryRepository;
  private EventStoreService eventStoreService;
  private DispatchService service;

  @BeforeEach
  void setUp() {
    target.succeed = true;
    target.calls = 0;
    subscriptionService = mock(SubscriptionService.class);
    deliveryRepository = mock(DeliveryRepository.class);
    eventStoreService = mock(EventStoreService.class);
    when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(deliveryRepository.findBySubscriptionIdAndEventId(anyString(), anyString()))
        .thenReturn(Optional.empty());
    service =
        new DispatchService(
            subscriptionService,
            new FilterExpressionEvaluator(),
            new PayloadTransformer(new ObjectMapper()),
            new DeliveryTargetRegistry(List.of(target)),
            new InMemoryStore(),
            deliveryRepository,
            eventStoreService,
            mock(EventTraceService.class),
            mock(AuditService.class));
  }

  @AfterEach
  void tearDown() {
    com.park.eventcenter.common.security.ParkContext.clear();
  }

  @Test
  void dispatchEventLoadsStoredEventAndRoutes() {
    com.park.eventcenter.common.security.ParkContext.set("PARK-A");
    when(eventStoreService.findCanonical("evt-1", "PARK-A")).thenReturn(Optional.of(event()));
    when(subscriptionService.findMatching("PARK-A", "access.denied"))
        .thenReturn(List.of(subscription("sub-1", "access.*", null, 0)));

    DispatchResult result = service.dispatchEvent("evt-1");

    assertThat(result.delivered()).isEqualTo(1);
    assertThat(target.calls).isEqualTo(1);
  }

  @Test
  void deliversToMatchingSubscription() {
    SubscriptionEntity subscription = subscription("sub-1", "access.*", null, 0);
    when(subscriptionService.findMatching("PARK-A", "access.denied"))
        .thenReturn(List.of(subscription));

    DispatchResult result = service.dispatch(event());

    assertThat(result.matched()).isEqualTo(1);
    assertThat(result.delivered()).isEqualTo(1);
    assertThat(target.calls).isEqualTo(1);
  }

  @Test
  void doesNotDeliverWhenFilterDoesNotMatch() {
    SubscriptionEntity subscription = subscription("sub-1", "access.*", "severity == 'INFO'", 0);
    when(subscriptionService.findMatching("PARK-A", "access.denied"))
        .thenReturn(List.of(subscription));

    DispatchResult result = service.dispatch(event());

    assertThat(result.matched()).isZero();
    assertThat(target.calls).isZero();
  }

  @Test
  void doesNotDeliverWhenNoSubscriptionMatches() {
    when(subscriptionService.findMatching(anyString(), anyString())).thenReturn(List.of());

    DispatchResult result = service.dispatch(event());

    assertThat(result.matched()).isZero();
    assertThat(target.calls).isZero();
  }

  @Test
  void retriesThenDeadLettersAfterMaxRetries() {
    target.succeed = false;
    SubscriptionEntity subscription = subscription("sub-1", "access.*", null, 2);
    when(subscriptionService.findMatching("PARK-A", "access.denied"))
        .thenReturn(List.of(subscription));

    DispatchResult result = service.dispatch(event());

    assertThat(target.calls).isEqualTo(3);
    assertThat(result.deadLettered()).isEqualTo(1);
    assertThat(result.delivered()).isZero();
  }

  @Test
  void repeatedDispatchOnlyTakesEffectOnce() {
    SubscriptionEntity subscription = subscription("sub-1", "access.*", null, 0);
    when(subscriptionService.findMatching("PARK-A", "access.denied"))
        .thenReturn(List.of(subscription));

    DispatchResult first = service.dispatch(event());
    DispatchResult second = service.dispatch(event());

    assertThat(first.delivered()).isEqualTo(1);
    assertThat(second.skipped()).isEqualTo(1);
    assertThat(target.calls).isEqualTo(1);
  }

  private SubscriptionEntity subscription(
      String id, String eventType, String filter, int maxRetries) {
    SubscriptionEntity subscription = new SubscriptionEntity();
    subscription.setId(id);
    subscription.setName("sub");
    subscription.setParkId("PARK-A");
    subscription.setEventType(eventType);
    subscription.setEnabled(true);
    subscription.setFilterExpression(filter);
    subscription.setTargetType("WEBHOOK");
    subscription.setTargetUrl("http://localhost/target");
    subscription.setMaxRetries(maxRetries);
    subscription.setBackoffMillis(0L);
    subscription.setCreatedAt(Instant.now());
    subscription.setUpdatedAt(Instant.now());
    return subscription;
  }

  private CanonicalEvent event() {
    return new CanonicalEvent(
        "evt-1",
        "access.denied",
        "gate-01",
        SourceType.ACCESS,
        Instant.parse("2026-01-02T03:04:05Z"),
        EventSeverity.MAJOR,
        "PARK-A",
        "Gate 1",
        Map.of("attempts", 3),
        null,
        "trace-1");
  }

  private static final class RecordingTarget implements DeliveryTarget {
    private int calls;
    private boolean succeed = true;

    @Override
    public boolean supports(String type) {
      return true;
    }

    @Override
    public DeliveryResult deliver(DeliveryRequest request) {
      calls++;
      return succeed ? DeliveryResult.ok() : DeliveryResult.failure("boom");
    }
  }

  private static final class InMemoryStore implements IdempotencyStore {
    private final Set<String> keys = new HashSet<>();

    @Override
    public boolean markIfAbsent(String key, Duration ttl) {
      return keys.add(key);
    }

    @Override
    public boolean exists(String key) {
      return keys.contains(key);
    }

    @Override
    public void mark(String key, Duration ttl) {
      keys.add(key);
    }
  }
}
