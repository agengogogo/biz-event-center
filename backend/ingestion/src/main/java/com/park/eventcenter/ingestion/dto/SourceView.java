package com.park.eventcenter.ingestion.dto;

import com.park.eventcenter.model.SourceType;
import java.time.Instant;

/** API representation of a registered source. The secret is only returned at creation time. */
public record SourceView(
    String id,
    String name,
    SourceType sourceType,
    String parkId,
    boolean enabled,
    String secret,
    Instant createdAt,
    Instant updatedAt) {}
