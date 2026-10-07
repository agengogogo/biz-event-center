package com.park.eventcenter.ingestion.dto;

import com.park.eventcenter.model.EventStatus;

/** Result of an ingestion attempt. */
public record IngestResponse(
    String eventId, String eventType, boolean accepted, boolean duplicate, EventStatus status) {}
