package com.park.eventcenter.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Uniform error payload returned by the management and ingestion APIs. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldViolation> fieldErrors) {

  /** A single field-level validation failure. */
  public record FieldViolation(String field, String message) {}

  public static ApiError of(int status, String error, String message, String path) {
    return new ApiError(Instant.now(), status, error, message, path, null);
  }

  public static ApiError of(
      int status, String error, String message, String path, List<FieldViolation> fieldErrors) {
    return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
  }
}
