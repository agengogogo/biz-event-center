package com.park.eventcenter.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.subscription.dto.TransformRule;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Applies declarative field-mapping rules to produce a transformed event payload. */
@Component
public class PayloadTransformer {

  private static final TypeReference<List<TransformRule>> RULE_LIST = new TypeReference<>() {};

  private final ObjectMapper mapper;

  public PayloadTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String serialize(List<TransformRule> rules) {
    if (rules == null || rules.isEmpty()) {
      return null;
    }
    try {
      return mapper.writeValueAsString(rules);
    } catch (JsonProcessingException ex) {
      throw new BadRequestException("Invalid transform rules: " + ex.getOriginalMessage());
    }
  }

  public List<TransformRule> parse(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return mapper.readValue(json, RULE_LIST);
    } catch (JsonProcessingException ex) {
      return List.of();
    }
  }

  /** Returns a copy of the event with its payload replaced by the transformed payload. */
  public CanonicalEvent apply(CanonicalEvent event, List<TransformRule> rules) {
    if (rules == null || rules.isEmpty()) {
      return event;
    }
    Map<String, Object> transformed = new LinkedHashMap<>();
    for (TransformRule rule : rules) {
      Object value = resolve(event, rule.from());
      if (value == null) {
        value = rule.defaultValue();
      }
      if (value != null && rule.to() != null && !rule.to().isBlank()) {
        setNested(transformed, rule.to(), value);
      }
    }
    return new CanonicalEvent(
        event.eventId(),
        event.eventType(),
        event.source(),
        event.sourceType(),
        event.occurredAt(),
        event.severity(),
        event.parkId(),
        event.location(),
        transformed,
        event.rawPayload(),
        event.traceId());
  }

  private Object resolve(CanonicalEvent event, String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    if (path.startsWith("payload.")) {
      return navigate(event.payload(), path.substring("payload.".length()));
    }
    return switch (path) {
      case "eventId" -> event.eventId();
      case "eventType" -> event.eventType();
      case "source" -> event.source();
      case "sourceType" -> event.sourceType() == null ? null : event.sourceType().name();
      case "occurredAt" -> event.occurredAt();
      case "severity" -> event.severity() == null ? null : event.severity().name();
      case "parkId" -> event.parkId();
      case "location" -> event.location();
      case "traceId" -> event.traceId();
      default -> null;
    };
  }

  private Object navigate(Map<String, Object> source, String dotted) {
    if (source == null) {
      return null;
    }
    Object current = source;
    for (String segment : dotted.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        return null;
      }
      current = map.get(segment);
    }
    return current;
  }

  @SuppressWarnings("unchecked")
  private void setNested(Map<String, Object> target, String path, Object value) {
    String[] segments = path.split("\\.");
    Map<String, Object> cursor = target;
    for (int i = 0; i < segments.length - 1; i++) {
      Object next = cursor.get(segments[i]);
      if (!(next instanceof Map)) {
        next = new LinkedHashMap<String, Object>();
        cursor.put(segments[i], next);
      }
      cursor = (Map<String, Object>) next;
    }
    cursor.put(segments[segments.length - 1], value);
  }
}
