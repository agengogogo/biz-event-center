package com.park.eventcenter.ingestion.service;

import com.park.eventcenter.model.PayloadValidationStatus;

/** Outcome of resolving a business event type and validating a payload against its schema. */
public record PayloadValidationResult(PayloadValidationStatus status, String reason) {

  public static PayloadValidationResult validated() {
    return new PayloadValidationResult(PayloadValidationStatus.VALIDATED, null);
  }

  public static PayloadValidationResult unvalidated(String reason) {
    return new PayloadValidationResult(PayloadValidationStatus.UNVALIDATED, reason);
  }
}
