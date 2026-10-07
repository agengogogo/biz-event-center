package com.park.eventcenter.dispatch.delivery;

import com.park.eventcenter.model.CanonicalEvent;

/** A single delivery attempt request for one subscription target. */
public record DeliveryRequest(
    CanonicalEvent event,
    String subscriptionId,
    String targetType,
    String targetUrl,
    String notificationChannelId,
    String templateId) {}
