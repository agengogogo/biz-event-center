package com.park.eventcenter.dispatch.dto;

/** Summary of one dispatch/redelivery pass. */
public record DispatchResult(
    String eventId, int matched, int delivered, int failed, int deadLettered, int skipped) {}
