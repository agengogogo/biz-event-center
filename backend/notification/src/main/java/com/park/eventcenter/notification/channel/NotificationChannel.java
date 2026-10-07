package com.park.eventcenter.notification.channel;

/** Pluggable outbound notification transport. */
public interface NotificationChannel {

  /** Channel type key, e.g. {@code WEBHOOK} or {@code WECHAT_WORK}. */
  String type();

  DeliveryResult send(NotificationMessage message);
}
