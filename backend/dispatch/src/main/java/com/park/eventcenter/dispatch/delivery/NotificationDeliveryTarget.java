package com.park.eventcenter.dispatch.delivery;

import com.park.eventcenter.notification.channel.DeliveryResult;
import com.park.eventcenter.notification.dto.DeliveryView;
import com.park.eventcenter.notification.service.NotificationService;
import org.springframework.stereotype.Component;

/** Delivers events by sending a notification through a configured channel. */
@Component
public class NotificationDeliveryTarget implements DeliveryTarget {

  public static final String TYPE = "NOTIFICATION";

  private final NotificationService notificationService;

  public NotificationDeliveryTarget(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Override
  public boolean supports(String type) {
    return TYPE.equalsIgnoreCase(type);
  }

  @Override
  public DeliveryResult deliver(DeliveryRequest request) {
    DeliveryView view =
        notificationService.send(
            request.event(),
            request.subscriptionId(),
            request.notificationChannelId(),
            request.templateId());
    return "SENT".equals(view.status())
        ? DeliveryResult.ok()
        : DeliveryResult.failure(view.error());
  }
}
