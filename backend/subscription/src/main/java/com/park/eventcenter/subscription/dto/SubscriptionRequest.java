package com.park.eventcenter.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Create/update payload for a subscription. */
public record SubscriptionRequest(
    @NotBlank String name,
    @NotBlank String eventType,
    Boolean enabled,
    String filterExpression,
    List<TransformRule> transformRules,
    @NotBlank String targetType,
    String targetUrl,
    String notificationChannelId,
    String templateId,
    Boolean orderedDelivery,
    Integer maxRetries,
    Long backoffMillis) {}
