package com.park.eventcenter.dispatch.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.dispatch.service.DispatchService;
import com.park.eventcenter.model.CanonicalEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drains the SQLite event outbox on a schedule.
 *
 * <p>Each pending canonical event is dispatched to matching subscriptions. A dispatch that throws
 * is retried with backoff and, after the configured maximum attempts, moved to a local dead-letter
 * state with the failure reason recorded — replacing Kafka without dropping events.
 */
@Component
@Profile("local")
public class OutboxDispatchPoller {

  static final String PENDING = "PENDING";
  static final String DISPATCHED = "DISPATCHED";
  static final String DEAD_LETTER = "DEAD_LETTER";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final DispatchService dispatchService;
  private final int batchSize;
  private final int maxAttempts;
  private final long backoffMillis;

  public OutboxDispatchPoller(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      DispatchService dispatchService,
      @Value("${event-center.local.outbox.batch-size:100}") int batchSize,
      @Value("${event-center.local.outbox.max-attempts:3}") int maxAttempts,
      @Value("${event-center.local.outbox.backoff-millis:1000}") long backoffMillis) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.dispatchService = dispatchService;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.backoffMillis = backoffMillis;
  }

  /** Polls the outbox and dispatches due pending events. */
  @Scheduled(fixedDelayString = "${event-center.local.outbox.poll-interval-ms:500}")
  public void poll() {
    long now = Instant.now().toEpochMilli();
    List<OutboxRow> rows =
        jdbcTemplate.query(
            "SELECT id, payload, attempts FROM event_outbox"
                + " WHERE status = 'PENDING'"
                + " AND (next_attempt_at IS NULL OR next_attempt_at <= ?)"
                + " ORDER BY id LIMIT ?",
            (rs, rowNum) ->
                new OutboxRow(rs.getLong("id"), rs.getString("payload"), rs.getInt("attempts")),
            now,
            batchSize);
    for (OutboxRow row : rows) {
      dispatch(row);
    }
  }

  private void dispatch(OutboxRow row) {
    try {
      CanonicalEvent event = objectMapper.readValue(row.payload(), CanonicalEvent.class);
      dispatchService.dispatch(event);
      jdbcTemplate.update(
          "UPDATE event_outbox SET status = ?, updated_at = ? WHERE id = ?",
          DISPATCHED,
          Instant.now().toEpochMilli(),
          row.id());
    } catch (Exception ex) {
      handleFailure(row, ex);
    }
  }

  private void handleFailure(OutboxRow row, Exception ex) {
    int attempts = row.attempts() + 1;
    String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    long now = Instant.now().toEpochMilli();
    if (attempts >= maxAttempts) {
      jdbcTemplate.update(
          "UPDATE event_outbox SET status = ?, attempts = ?, last_error = ?, updated_at = ?"
              + " WHERE id = ?",
          DEAD_LETTER,
          attempts,
          truncate(error),
          now,
          row.id());
    } else {
      jdbcTemplate.update(
          "UPDATE event_outbox SET attempts = ?, last_error = ?, next_attempt_at = ?,"
              + " updated_at = ? WHERE id = ?",
          attempts,
          truncate(error),
          now + backoffMillis * attempts,
          now,
          row.id());
    }
  }

  private static String truncate(String value) {
    return value.length() > 2000 ? value.substring(0, 2000) : value;
  }

  private record OutboxRow(long id, String payload, int attempts) {}
}
