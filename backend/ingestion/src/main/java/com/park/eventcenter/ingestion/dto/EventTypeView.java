package com.park.eventcenter.ingestion.dto;

import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import java.time.Instant;

/** API representation of a business event type definition and its payload schema. */
public record EventTypeView(
    String type,
    String name,
    SourceType sourceCategory,
    EventSeverity defaultSeverity,
    String payloadSchema,
    boolean enabled,
    int version,
    Instant createdAt,
    Instant updatedAt) {}
