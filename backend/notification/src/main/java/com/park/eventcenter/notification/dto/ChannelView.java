package com.park.eventcenter.notification.dto;

import java.time.Instant;
import java.util.Map;

/** API representation of a notification channel. */
public record ChannelView(
    String id,
    String name,
    String type,
    String parkId,
    Map<String, Object> config,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {}
