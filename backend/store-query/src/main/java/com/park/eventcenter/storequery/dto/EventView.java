package com.park.eventcenter.storequery.dto;

import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.EventStatus;
import com.park.eventcenter.model.PayloadValidationStatus;
import com.park.eventcenter.model.SourceType;
import java.time.Instant;
import java.util.Map;

/** API representation of a stored event with its standardized and raw payloads. */
public record EventView(
    String eventId,
    String eventType,
    String source,
    SourceType sourceType,
    Instant occurredAt,
    EventSeverity severity,
    String parkId,
    String location,
    Map<String, Object> payload,
    Map<String, Object> rawPayload,
    String traceId,
    EventStatus status,
    PayloadValidationStatus eventTypeValidation,
    String validationReason,
    Instant receivedAt,
    Instant updatedAt) {}
