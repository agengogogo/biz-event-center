package com.park.eventcenter.common.error;

import org.springframework.http.HttpStatus;

/** Thrown when credentials are missing or invalid. */
public class UnauthorizedException extends ApiException {

  public UnauthorizedException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }
}
