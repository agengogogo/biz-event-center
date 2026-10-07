package com.park.eventcenter.dispatch.dto;

import java.time.Instant;

/** API representation of a delivery attempt. */
public record DeliveryView(
    Long id,
    String parkId,
    String eventId,
    String subscriptionId,
    String targetType,
    String target,
    String status,
    int attempts,
    String lastError,
    boolean deadLetter,
    Instant createdAt,
    Instant updatedAt) {}
