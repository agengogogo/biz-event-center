package com.park.eventcenter.subscription.service;

import com.park.eventcenter.common.error.NotFoundException;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.subscription.dto.SubscriptionRequest;
import com.park.eventcenter.subscription.dto.SubscriptionView;
import com.park.eventcenter.subscription.dto.TransformRule;
import com.park.eventcenter.subscription.entity.SubscriptionEntity;
import com.park.eventcenter.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Subscription lifecycle management and topic matching. */
@Service
public class SubscriptionService {

  private final SubscriptionRepository repository;
  private final FilterExpressionEvaluator filterEvaluator;
  private final PayloadTransformer payloadTransformer;

  public SubscriptionService(
      SubscriptionRepository repository,
      FilterExpressionEvaluator filterEvaluator,
      PayloadTransformer payloadTransformer) {
    this.repository = repository;
    this.filterEvaluator = filterEvaluator;
    this.payloadTransformer = payloadTransformer;
  }

  @Transactional
  public SubscriptionView create(SubscriptionRequest request) {
    SubscriptionEntity entity = new SubscriptionEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setParkId(ParkContext.require());
    entity.setCreatedAt(Instant.now());
    apply(entity, request);
    return toView(repository.save(entity));
  }

  @Transactional
  public SubscriptionView update(String id, SubscriptionRequest request) {
    SubscriptionEntity entity = require(id);
    apply(entity, request);
    return toView(repository.save(entity));
  }

  @Transactional(readOnly = true)
  public List<SubscriptionView> list() {
    return repository.findByParkIdOrderByCreatedAtDesc(ParkContext.require()).stream()
        .map(this::toView)
        .toList();
  }

  @Transactional(readOnly = true)
  public SubscriptionView get(String id) {
    return toView(require(id));
  }

  @Transactional
  public SubscriptionView setEnabled(String id, boolean enabled) {
    SubscriptionEntity entity = require(id);
    entity.setEnabled(enabled);
    entity.setUpdatedAt(Instant.now());
    return toView(repository.save(entity));
  }

  @Transactional
  public void delete(String id) {
    repository.delete(require(id));
  }

  @Transactional(readOnly = true)
  public List<SubscriptionEntity> findMatching(String parkId, String eventType) {
    return repository.findByParkIdAndEnabledTrue(parkId).stream()
        .filter(subscription -> matchesTopic(subscription.getEventType(), eventType))
        .toList();
  }

  /** Whether an event type matches a subscription topic (exact, {@code *}, or {@code prefix.*}). */
  public boolean matchesTopic(String pattern, String eventType) {
    if (pattern == null || pattern.isBlank() || "*".equals(pattern)) {
      return true;
    }
    if (pattern.endsWith(".*")) {
      return eventType != null && eventType.startsWith(pattern.substring(0, pattern.length() - 1));
    }
    return pattern.equals(eventType);
  }

  @Transactional(readOnly = true)
  public SubscriptionEntity require(String id) {
    return repository
        .findByIdAndParkId(id, ParkContext.require())
        .orElseThrow(() -> new NotFoundException("Subscription not found: " + id));
  }

  private void apply(SubscriptionEntity entity, SubscriptionRequest request) {
    filterEvaluator.validate(request.filterExpression());
    entity.setName(request.name());
    entity.setEventType(request.eventType());
    entity.setEnabled(request.enabled() == null || request.enabled());
    entity.setFilterExpression(
        request.filterExpression() == null || request.filterExpression().isBlank()
            ? null
            : request.filterExpression());
    entity.setTransformRules(payloadTransformer.serialize(request.transformRules()));
    entity.setTargetType(request.targetType());
    entity.setTargetUrl(request.targetUrl());
    entity.setNotificationChannelId(request.notificationChannelId());
    entity.setTemplateId(request.templateId());
    entity.setOrderedDelivery(Boolean.TRUE.equals(request.orderedDelivery()));
    entity.setMaxRetries(request.maxRetries() == null ? 3 : Math.max(request.maxRetries(), 0));
    entity.setBackoffMillis(
        request.backoffMillis() == null ? 1000L : Math.max(request.backoffMillis(), 0L));
    entity.setUpdatedAt(Instant.now());
  }

  private SubscriptionView toView(SubscriptionEntity entity) {
    List<TransformRule> rules = payloadTransformer.parse(entity.getTransformRules());
    return new SubscriptionView(
        entity.getId(),
        entity.getName(),
        entity.getParkId(),
        entity.getEventType(),
        entity.isEnabled(),
        entity.getFilterExpression(),
        rules,
        entity.getTargetType(),
        entity.getTargetUrl(),
        entity.getNotificationChannelId(),
        entity.getTemplateId(),
        entity.isOrderedDelivery(),
        entity.getMaxRetries(),
        entity.getBackoffMillis(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
