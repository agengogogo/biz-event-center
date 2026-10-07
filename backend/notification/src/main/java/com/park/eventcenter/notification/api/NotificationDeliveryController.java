package com.park.eventcenter.notification.api;

import com.park.eventcenter.notification.dto.DeliveryView;
import com.park.eventcenter.notification.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Notification delivery tracking and manual resend endpoints. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationDeliveryController {

  private final NotificationService service;

  public NotificationDeliveryController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  public List<DeliveryView> list(@RequestParam(required = false) String eventId) {
    return eventId == null ? service.list() : service.listByEvent(eventId);
  }

  @PostMapping("/{id}/resend")
  public DeliveryView resend(@PathVariable Long id) {
    return service.resend(id);
  }
}
