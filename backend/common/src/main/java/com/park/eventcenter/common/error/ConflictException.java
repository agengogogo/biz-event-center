package com.park.eventcenter.common.error;

import org.springframework.http.HttpStatus;

/** Thrown when a request conflicts with the current state of a resource. */
public class ConflictException extends ApiException {

  public ConflictException(String message) {
    super(HttpStatus.CONFLICT, message);
  }
}
