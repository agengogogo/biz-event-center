package com.park.eventcenter.subscription.dto;

import java.time.Instant;
import java.util.List;

/** API representation of a subscription. */
public record SubscriptionView(
    String id,
    String name,
    String parkId,
    String eventType,
    boolean enabled,
    String filterExpression,
    List<TransformRule> transformRules,
    String targetType,
    String targetUrl,
    String notificationChannelId,
    String templateId,
    boolean orderedDelivery,
    int maxRetries,
    long backoffMillis,
    Instant createdAt,
    Instant updatedAt) {}
