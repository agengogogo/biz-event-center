package com.park.eventcenter.common.error;

import com.park.eventcenter.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates exceptions into the shared {@link ApiError} JSON shape. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
    HttpStatus status = ex.getStatus();
    return ResponseEntity.status(status)
        .body(
            ApiError.of(status.value(), status.getReasonPhrase(), ex.getMessage(), path(request)));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ApiError.FieldViolation> violations =
        ex.getBindingResult().getFieldErrors().stream().map(this::toViolation).toList();
    return ResponseEntity.badRequest()
        .body(
            ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                path(request),
                violations));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraint(
      ConstraintViolationException ex, HttpServletRequest request) {
    List<ApiError.FieldViolation> violations =
        ex.getConstraintViolations().stream()
            .map(v -> new ApiError.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
            .toList();
    return ResponseEntity.badRequest()
        .body(
            ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                path(request),
                violations));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.FORBIDDEN;
    return ResponseEntity.status(status)
        .body(
            ApiError.of(status.value(), status.getReasonPhrase(), ex.getMessage(), path(request)));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(
      AuthenticationException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    return ResponseEntity.status(status)
        .body(
            ApiError.of(status.value(), status.getReasonPhrase(), ex.getMessage(), path(request)));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(status)
        .body(
            ApiError.of(status.value(), status.getReasonPhrase(), ex.getMessage(), path(request)));
  }

  private ApiError.FieldViolation toViolation(FieldError error) {
    return new ApiError.FieldViolation(error.getField(), error.getDefaultMessage());
  }

  private String path(HttpServletRequest request) {
    return request.getRequestURI();
  }
}
