package com.park.eventcenter.storequery.dto;

import com.park.eventcenter.model.EventSeverity;
import java.time.Instant;

/** Filter criteria for conditional event search. All fields are optional. */
public record EventSearchCriteria(
    Instant from,
    Instant to,
    String eventType,
    String source,
    EventSeverity severity,
    String location,
    int page,
    int size) {}
