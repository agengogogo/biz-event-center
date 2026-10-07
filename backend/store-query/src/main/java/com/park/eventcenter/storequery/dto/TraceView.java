package com.park.eventcenter.storequery.dto;

import java.time.Instant;

/** One entry of an event's processing timeline. */
public record TraceView(String stage, String status, String detail, Instant occurredAt) {}
