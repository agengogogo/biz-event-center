package com.park.eventcenter.ingestion.dto;

import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;

/** Admin request to define or update a business event type. */
public record EventTypeRequest(
    String type,
    String name,
    SourceType sourceCategory,
    EventSeverity defaultSeverity,
    String payloadSchema,
    Boolean enabled) {}
