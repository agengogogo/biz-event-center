package com.park.eventcenter.dispatch.delivery;

import com.park.eventcenter.notification.channel.DeliveryResult;

/** Pluggable delivery target (HTTP webhook, notification channel, ...). */
public interface DeliveryTarget {

  boolean supports(String type);

  DeliveryResult deliver(DeliveryRequest request);
}
