package com.park.eventcenter.notification.channel;

/** Outcome of a notification send attempt. */
public record DeliveryResult(boolean success, String error) {

  public static DeliveryResult ok() {
    return new DeliveryResult(true, null);
  }

  public static DeliveryResult failure(String error) {
    return new DeliveryResult(false, error);
  }
}
