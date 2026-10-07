package com.park.eventcenter.dispatch.api;

import com.park.eventcenter.dispatch.dto.DeliveryView;
import com.park.eventcenter.dispatch.dto.DispatchResult;
import com.park.eventcenter.dispatch.service.DispatchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API-triggered dispatch, redelivery and delivery/dead-letter inspection. */
@RestController
@RequestMapping("/api")
public class DispatchController {

  private final DispatchService dispatchService;

  public DispatchController(DispatchService dispatchService) {
    this.dispatchService = dispatchService;
  }

  @PostMapping("/events/{eventId}/dispatch")
  public DispatchResult dispatch(@PathVariable String eventId) {
    return dispatchService.dispatchEvent(eventId);
  }

  @PostMapping("/events/{eventId}/redeliver")
  public DispatchResult redeliver(@PathVariable String eventId) {
    return dispatchService.redeliver(eventId);
  }

  @GetMapping("/deliveries")
  public List<DeliveryView> deliveries() {
    return dispatchService.list();
  }

  @GetMapping("/deliveries/dead-letters")
  public List<DeliveryView> deadLetters() {
    return dispatchService.listDeadLetters();
  }

  @PostMapping("/deliveries/{id}/redeliver")
  public DispatchResult redeliverDelivery(@PathVariable Long id) {
    return dispatchService.redeliverDelivery(id);
  }
}
