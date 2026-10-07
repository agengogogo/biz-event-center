package com.park.eventcenter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs the shared API contract scenario set under the {@code local} demo profile: embedded SQLite
 * for persistence, dedup and the event bus, with no PostgreSQL, Redis or Kafka. This is the
 * always-on half of the two-profile contract check; {@link EventCenterIntegrationTest} runs the
 * same scenarios against the production backends when {@code BEC_IT=true}.
 */
@ActiveProfiles("local")
class LocalProfileContractTest extends AbstractApiContractTest {

  private static final Path DATABASE = newDatabaseFile();

  private static Path newDatabaseFile() {
    try {
      return Files.createTempDirectory("bec-local-").resolve("eventcenter.db");
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @DynamicPropertySource
  static void localProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () -> "jdbc:sqlite:" + DATABASE.toAbsolutePath() + "?journal_mode=WAL&busy_timeout=10000");
    registry.add("event-center.local.outbox.poll-interval-ms", () -> "200");
    registry.add("event-center.local.outbox.backoff-millis", () -> "50");
    registry.add("event-center.local.idempotency.cleanup-interval-ms", () -> "1000");
  }
}
