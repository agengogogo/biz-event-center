package com.park.eventcenter.dispatch.bus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.bus.EventBus;
import com.park.eventcenter.model.CanonicalEvent;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * SQLite-backed outbox {@link EventBus} adapter used by the {@code local} demo profile.
 *
 * <p>Instead of handing the event to a broker, it durably appends it to the {@code event_outbox}
 * table. {@link OutboxDispatchPoller} later drains the outbox, so events survive restarts and the
 * local bus keeps the durable-buffer, retry and dead-letter semantics of the Kafka path.
 */
@Component
@Profile("local")
public class SqliteOutboxEventBus implements EventBus {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public SqliteOutboxEventBus(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(CanonicalEvent event) {
    String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Cannot serialize canonical event " + event.eventId(), ex);
    }
    long now = Instant.now().toEpochMilli();
    jdbcTemplate.update(
        "INSERT INTO event_outbox"
            + " (event_id, park_id, payload, status, attempts, next_attempt_at, created_at, updated_at)"
            + " VALUES (?, ?, ?, 'PENDING', 0, ?, ?, ?)",
        event.eventId(),
        event.parkId(),
        payload,
        now,
        now,
        now);
  }
}
