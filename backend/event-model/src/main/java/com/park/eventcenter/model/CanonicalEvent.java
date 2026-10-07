package com.park.eventcenter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The canonical event envelope shared by ingestion, dispatch, storage, notification and dashboards.
 * Payload is free-form; rawPayload preserves the original source document for traceability.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CanonicalEvent(
    String eventId,
    String eventType,
    String source,
    SourceType sourceType,
    Instant occurredAt,
    EventSeverity severity,
    String parkId,
    String location,
    java.util.Map<String, Object> payload,
    java.util.Map<String, Object> rawPayload,
    String traceId) {

  public CanonicalEvent {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(eventType, "eventType");
    Objects.requireNonNull(sourceType, "sourceType");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(parkId, "parkId");
  }

  public static String newId() {
    return UUID.randomUUID().toString();
  }
}
