package com.park.eventcenter.ingestion.dto;

import java.util.Map;

/** Inbound event submission. Unknown envelope fields are preserved in rawPayload. */
public record IngestRequest(
    String eventId,
    String eventType,
    Object occurredAt,
    String severity,
    String location,
    Map<String, Object> payload,
    String traceId) {}
