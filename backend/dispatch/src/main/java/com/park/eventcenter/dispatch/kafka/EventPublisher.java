package com.park.eventcenter.dispatch.kafka;

import com.park.eventcenter.common.bus.EventBus;
import com.park.eventcenter.model.CanonicalEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed {@link EventBus} adapter used by the production profile.
 *
 * <p>Publishes canonical events to the event bus, keyed by park and event type for partitioning.
 */
@Component
@Profile("!local")
public class EventPublisher implements EventBus {

  private final KafkaTemplate<String, CanonicalEvent> kafkaTemplate;

  public EventPublisher(KafkaTemplate<String, CanonicalEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public void publish(CanonicalEvent event) {
    String key = event.parkId() + ":" + event.eventType();
    kafkaTemplate.send(EventTopics.EVENTS, key, event);
  }
}
