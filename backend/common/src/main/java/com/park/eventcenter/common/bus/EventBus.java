package com.park.eventcenter.common.bus;

import com.park.eventcenter.model.CanonicalEvent;

/**
 * Infrastructure port for the canonical-event bus.
 *
 * <p>The production profile wires the Kafka adapter; the {@code local} demo profile wires a
 * SQLite-backed outbox adapter. Business modules depend only on this port, never on a broker
 * client.
 */
public interface EventBus {

  /** Publishes a canonical event to the bus for downstream routing. */
  void publish(CanonicalEvent event);
}
