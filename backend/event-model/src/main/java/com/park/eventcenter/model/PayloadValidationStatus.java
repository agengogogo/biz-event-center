package com.park.eventcenter.model;

/** Outcome of validating an event's payload against its business event type schema. */
public enum PayloadValidationStatus {
  VALIDATED,
  UNVALIDATED
}
