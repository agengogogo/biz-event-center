package com.park.eventcenter.model;

/** Lifecycle status of a persisted canonical event. */
public enum EventStatus {
  RECEIVED,
  DISPATCHED,
  DELIVERED,
  FAILED,
  DEAD_LETTER,
  CLOSED
}
