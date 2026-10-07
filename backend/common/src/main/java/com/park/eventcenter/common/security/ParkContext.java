package com.park.eventcenter.common.security;

import com.park.eventcenter.common.error.ForbiddenException;
import java.util.Optional;

/**
 * Thread-bound holder for the park id of the current request. Services must read it from here so
 * that cross-park access is rejected in the data access layer rather than only in the UI.
 */
public final class ParkContext {

  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private ParkContext() {}

  public static void set(String parkId) {
    CURRENT.set(parkId);
  }

  public static Optional<String> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  /** Returns the current park id or fails the request. */
  public static String require() {
    String parkId = CURRENT.get();
    if (parkId == null || parkId.isBlank()) {
      throw new ForbiddenException("No park context bound to the current request");
    }
    return parkId;
  }

  /** Asserts the given resource park id matches the caller's park. */
  public static void assertAccess(String resourceParkId) {
    String current = CURRENT.get();
    if (current == null || !current.equals(resourceParkId)) {
      throw new ForbiddenException("Cross-park access denied");
    }
  }

  public static void clear() {
    CURRENT.remove();
  }
}
