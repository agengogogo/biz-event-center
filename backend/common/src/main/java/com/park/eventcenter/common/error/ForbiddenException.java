package com.park.eventcenter.common.error;

import org.springframework.http.HttpStatus;

/** Thrown when an authenticated caller attempts a cross-park or disallowed operation. */
public class ForbiddenException extends ApiException {

  public ForbiddenException(String message) {
    super(HttpStatus.FORBIDDEN, message);
  }
}
