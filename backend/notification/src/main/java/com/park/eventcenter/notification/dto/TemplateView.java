package com.park.eventcenter.notification.dto;

import java.time.Instant;

/** API representation of a notification template. */
public record TemplateView(
    String id,
    String name,
    String channelType,
    String subject,
    String body,
    String parkId,
    Instant createdAt,
    Instant updatedAt) {}
