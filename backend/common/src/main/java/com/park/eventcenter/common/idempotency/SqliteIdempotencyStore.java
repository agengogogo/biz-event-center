package com.park.eventcenter.common.idempotency;

import java.time.Duration;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SQLite-backed idempotency store used by the {@code local} demo profile.
 *
 * <p>Keys and their expiry live in the {@code idempotency_key} table so dedup survives restarts
 * without Redis. Expired rows are purged on a schedule to keep the embedded table bounded.
 */
@Component
@Profile("local")
public class SqliteIdempotencyStore implements IdempotencyStore {

  private final JdbcTemplate jdbcTemplate;

  public SqliteIdempotencyStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean markIfAbsent(String key, Duration ttl) {
    long now = Instant.now().toEpochMilli();
    long expiresAt = now + Math.max(ttl.toMillis(), 0L);
    int inserted =
        jdbcTemplate.update(
            "INSERT OR IGNORE INTO idempotency_key (dedup_key, expires_at) VALUES (?, ?)",
            key,
            expiresAt);
    if (inserted == 1) {
      return true;
    }
    int reclaimed =
        jdbcTemplate.update(
            "UPDATE idempotency_key SET expires_at = ? WHERE dedup_key = ? AND expires_at <= ?",
            expiresAt,
            key,
            now);
    return reclaimed == 1;
  }

  @Override
  public boolean exists(String key) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM idempotency_key WHERE dedup_key = ? AND expires_at > ?",
            Integer.class,
            key,
            Instant.now().toEpochMilli());
    return count != null && count > 0;
  }

  @Override
  public void mark(String key, Duration ttl) {
    jdbcTemplate.update(
        "INSERT OR REPLACE INTO idempotency_key (dedup_key, expires_at) VALUES (?, ?)",
        key,
        Instant.now().toEpochMilli() + Math.max(ttl.toMillis(), 0L));
  }

  /** Removes expired keys so the embedded table does not grow without bound. */
  @Scheduled(fixedDelayString = "${event-center.local.idempotency.cleanup-interval-ms:600000}")
  public void purgeExpired() {
    jdbcTemplate.update(
        "DELETE FROM idempotency_key WHERE expires_at <= ?", Instant.now().toEpochMilli());
  }
}
