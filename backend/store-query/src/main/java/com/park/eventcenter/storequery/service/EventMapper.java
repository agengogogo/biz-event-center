package com.park.eventcenter.storequery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.storequery.dto.EventView;
import com.park.eventcenter.storequery.entity.EventEntity;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Converts between canonical events, JSON payloads and stored views. */
@Component
public class EventMapper {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final ObjectMapper mapper;

  public EventMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String writeMap(Map<String, Object> value) {
    if (value == null) {
      return null;
    }
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize payload", ex);
    }
  }

  public Map<String, Object> readMap(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return mapper.readValue(json, MAP_TYPE);
    } catch (JsonProcessingException ex) {
      return Map.of();
    }
  }

  public EventView toView(EventEntity entity) {
    return new EventView(
        entity.getEventId(),
        entity.getEventType(),
        entity.getSource(),
        entity.getSourceType(),
        entity.getOccurredAt(),
        entity.getSeverity(),
        entity.getParkId(),
        entity.getLocation(),
        readMap(entity.getPayload()),
        readMap(entity.getRawPayload()),
        entity.getTraceId(),
        entity.getStatus(),
        entity.getEventTypeValidation(),
        entity.getValidationReason(),
        entity.getReceivedAt(),
        entity.getUpdatedAt());
  }

  public CanonicalEvent toCanonical(EventEntity entity) {
    return new CanonicalEvent(
        entity.getEventId(),
        entity.getEventType(),
        entity.getSource(),
        entity.getSourceType(),
        entity.getOccurredAt(),
        entity.getSeverity(),
        entity.getParkId(),
        entity.getLocation(),
        readMap(entity.getPayload()),
        readMap(entity.getRawPayload()),
        entity.getTraceId());
  }
}
