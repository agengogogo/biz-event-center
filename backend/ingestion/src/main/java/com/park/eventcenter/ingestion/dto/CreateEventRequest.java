package com.park.eventcenter.ingestion.dto;

import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import java.time.Instant;
import java.util.Map;

/** Admin request to create a canonical event directly through the management API. */
public record CreateEventRequest(
    String eventId,
    String eventType,
    Instant occurredAt,
    EventSeverity severity,
    String location,
    SourceType sourceType,
    Map<String, Object> payload,
    String traceId) {}
