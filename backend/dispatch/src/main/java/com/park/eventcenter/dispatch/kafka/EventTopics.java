package com.park.eventcenter.dispatch.kafka;

/** Kafka topic names for the event bus and its dead-letter topic. */
public final class EventTopics {

  public static final String EVENTS = "park.events";
  public static final String EVENTS_DLT = "park.events.dlt";

  private EventTopics() {}
}
