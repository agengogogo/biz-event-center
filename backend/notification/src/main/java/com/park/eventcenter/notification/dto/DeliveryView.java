package com.park.eventcenter.notification.dto;

import java.time.Instant;

/** API representation of a notification delivery record. */
public record DeliveryView(
    Long id,
    String parkId,
    String eventId,
    String subscriptionId,
    String channelId,
    String status,
    String error,
    String renderedContent,
    int attempts,
    Instant createdAt,
    Instant updatedAt) {}
