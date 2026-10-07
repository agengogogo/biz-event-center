package com.park.eventcenter.model;

/** Ordered stages recorded on an event's processing timeline. */
public enum TraceStage {
  INGESTED,
  VALIDATED,
  TYPE_VALIDATED,
  DEDUPLICATED,
  PERSISTED,
  PUBLISHED,
  ROUTED,
  DELIVERED,
  NOTIFIED,
  RETRIED,
  DEAD_LETTERED,
  REDELIVERED
}
