package com.park.eventcenter.dispatch.kafka;

import com.park.eventcenter.dispatch.service.DispatchService;
import com.park.eventcenter.model.CanonicalEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes canonical events from the bus and routes them to matching subscriptions. */
@Component
@Profile("!local")
public class EventDispatchListener {

  private final DispatchService dispatchService;

  public EventDispatchListener(DispatchService dispatchService) {
    this.dispatchService = dispatchService;
  }

  @KafkaListener(
      topics = EventTopics.EVENTS,
      groupId = "${spring.kafka.consumer.group-id:biz-event-center}")
  public void onEvent(CanonicalEvent event) {
    dispatchService.dispatch(event);
  }
}
