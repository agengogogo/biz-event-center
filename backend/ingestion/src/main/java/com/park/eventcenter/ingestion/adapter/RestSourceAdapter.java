package com.park.eventcenter.ingestion.adapter;

import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.ingestion.entity.SourceEntity;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * First-version REST adapter. It maps well-known envelope fields (with aliases) to the canonical
 * model and preserves the entire raw document for traceability.
 */
@Component
public class RestSourceAdapter implements SourceAdapter {

  @Override
  public boolean supports(SourceType sourceType) {
    return true;
  }

  @Override
  public CanonicalEvent adapt(SourceEntity source, Map<String, Object> raw) {
    Map<String, Object> body = raw == null ? Map.of() : raw;
    String eventType = firstString(body, "eventType", "event_type", "type", "topic");
    if (eventType == null || eventType.isBlank()) {
      throw new BadRequestException("eventType is required");
    }
    Instant occurredAt =
        parseInstant(first(body, "occurredAt", "occurred_at", "timestamp", "time", "ts"));
    if (occurredAt == null) {
      throw new BadRequestException("occurredAt is required");
    }
    String eventId = firstString(body, "eventId", "event_id", "id", "uuid");
    if (eventId == null || eventId.isBlank()) {
      eventId = UUID.randomUUID().toString();
    }
    EventSeverity severity = parseSeverity(firstString(body, "severity", "level"));
    String location = firstString(body, "location", "place", "area");
    String traceId = firstString(body, "traceId", "trace_id");
    Map<String, Object> payload = extractPayload(body);

    return new CanonicalEvent(
        eventId,
        eventType,
        source.getId(),
        source.getSourceType(),
        occurredAt,
        severity,
        source.getParkId(),
        location,
        payload,
        new LinkedHashMap<>(body),
        traceId);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> extractPayload(Map<String, Object> body) {
    Object payload = body.get("payload");
    if (payload instanceof Map<?, ?> map) {
      return new LinkedHashMap<>((Map<String, Object>) map);
    }
    Object data = body.get("data");
    if (data instanceof Map<?, ?> map) {
      return new LinkedHashMap<>((Map<String, Object>) map);
    }
    return new LinkedHashMap<>(body);
  }

  private Object first(Map<String, Object> body, String... keys) {
    for (String key : keys) {
      if (body.containsKey(key) && body.get(key) != null) {
        return body.get(key);
      }
    }
    return null;
  }

  private String firstString(Map<String, Object> body, String... keys) {
    Object value = first(body, keys);
    return value == null ? null : String.valueOf(value);
  }

  private EventSeverity parseSeverity(String value) {
    if (value == null || value.isBlank()) {
      return EventSeverity.WARNING;
    }
    try {
      return EventSeverity.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return EventSeverity.WARNING;
    }
  }

  private Instant parseInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return Instant.ofEpochMilli(number.longValue());
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return null;
    }
    try {
      return Instant.parse(text);
    } catch (DateTimeParseException ignored) {
      // try other formats below
    }
    try {
      return OffsetDateTime.parse(text).toInstant();
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      return Instant.ofEpochMilli(Long.parseLong(text));
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
