package com.park.eventcenter.subscription.service;

import com.park.eventcenter.model.CanonicalEvent;
import java.time.Instant;
import java.util.Map;

/** Safe, read-only view of a canonical event exposed to filter expressions. */
public class EventExpressionRoot {

  private final CanonicalEvent event;

  public EventExpressionRoot(CanonicalEvent event) {
    this.event = event;
  }

  public String getEventId() {
    return event.eventId();
  }

  public String getEventType() {
    return event.eventType();
  }

  public String getSource() {
    return event.source();
  }

  public String getSourceType() {
    return event.sourceType() == null ? null : event.sourceType().name();
  }

  public Instant getOccurredAt() {
    return event.occurredAt();
  }

  public String getSeverity() {
    return event.severity() == null ? null : event.severity().name();
  }

  public String getParkId() {
    return event.parkId();
  }

  public String getLocation() {
    return event.location();
  }

  public String getTraceId() {
    return event.traceId();
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getPayload() {
    return event.payload() == null ? Map.of() : (Map<String, Object>) (Map<?, ?>) event.payload();
  }

  public boolean contains(String value, String token) {
    return value != null && token != null && value.contains(token);
  }

  public boolean startsWith(String value, String prefix) {
    return value != null && prefix != null && value.startsWith(prefix);
  }

  public boolean endsWith(String value, String suffix) {
    return value != null && suffix != null && value.endsWith(suffix);
  }

  public boolean matches(String value, String regex) {
    return value != null && regex != null && value.matches(regex);
  }

  public int len(String value) {
    return value == null ? 0 : value.length();
  }

  public String upper(String value) {
    return value == null ? null : value.toUpperCase();
  }

  public String lower(String value) {
    return value == null ? null : value.toLowerCase();
  }

  public boolean has(String field) {
    if (field == null) {
      return false;
    }
    return switch (field) {
      case "eventId" -> event.eventId() != null;
      case "eventType" -> event.eventType() != null;
      case "source" -> event.source() != null;
      case "sourceType" -> event.sourceType() != null;
      case "occurredAt" -> event.occurredAt() != null;
      case "severity" -> event.severity() != null;
      case "parkId" -> event.parkId() != null;
      case "location" -> event.location() != null;
      case "traceId" -> event.traceId() != null;
      case "payload" -> event.payload() != null;
      default -> false;
    };
  }
}
