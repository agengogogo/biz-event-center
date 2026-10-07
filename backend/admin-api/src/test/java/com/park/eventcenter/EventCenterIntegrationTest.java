package com.park.eventcenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Production-profile half of the two-profile API contract check. It inherits the shared scenario
 * set from {@link AbstractApiContractTest} and adds product-specific end-to-end coverage.
 *
 * <p>Requires PostgreSQL and Redis reachable at the application's configured endpoints (for example
 * the repository docker-compose stack) and runs an in-JVM Kafka broker. Enable with {@code
 * BEC_IT=true} so unit-only CI stays green; {@link LocalProfileContractTest} covers the same
 * scenarios without external infrastructure.
 */
@EnabledIfEnvironmentVariable(named = "BEC_IT", matches = "true")
class EventCenterIntegrationTest extends AbstractApiContractTest {

  private static EmbeddedKafkaBroker kafkaBroker;

  static EmbeddedKafkaBroker kafka() {
    if (kafkaBroker == null) {
      EmbeddedKafkaBroker broker = new EmbeddedKafkaZKBroker(1);
      broker.kafkaPorts(0);
      broker.afterPropertiesSet();
      kafkaBroker = broker;
    }
    return kafkaBroker;
  }

  @DynamicPropertySource
  static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", () -> kafka().getBrokersAsString());
  }

  @Test
  void notificationChannelTemplateAndResend() throws Exception {
    String token = login("admin", "admin123");
    var channel =
        postJson(
            token,
            "/api/notification-channels",
            Map.of(
                "name",
                "ops-webhook-" + UUID.randomUUID(),
                "type",
                "WEBHOOK",
                "config",
                Map.of("url", webhookUrl())));
    String channelId = channel.get("id").asText();
    var template =
        postJson(
            token,
            "/api/notification-templates",
            Map.of(
                "name", "alert-" + UUID.randomUUID(),
                "channelType", "WEBHOOK",
                "subject", "Alert ${eventType}",
                "body", "Event ${eventType} at ${location}"));
    String templateId = template.get("id").asText();

    createSubscription(
        token,
        "notify-sub-" + UUID.randomUUID(),
        "notify.*",
        "NOTIFICATION",
        null,
        channelId,
        templateId,
        0);

    var source = createSource(token, "notify-sensor");
    String eventId = "it-" + UUID.randomUUID();
    ingest(source.get("id").asText(), source.get("secret").asText(), body(eventId, "notify.alarm"));

    boolean sent =
        await(
            Duration.ofSeconds(20),
            () ->
                notificationDeliveryRepository
                    .findByEventIdAndParkIdOrderByCreatedAtDesc(eventId, "PARK-A")
                    .stream()
                    .anyMatch(d -> "SENT".equals(d.getStatus())));
    assertThat(sent).as("notification delivered through configured channel").isTrue();
  }

  @Test
  void performanceSmoke() throws Exception {
    String token = login("admin", "admin123");
    var source = createSource(token, "perf-sensor");
    int count = 30;
    long start = System.nanoTime();
    for (int i = 0; i < count; i++) {
      var response =
          ingest(
              source.get("id").asText(),
              source.get("secret").asText(),
              body("it-perf-" + UUID.randomUUID(), "metrics.load"));
      assertThat(response.get("accepted").asBoolean()).isTrue();
    }
    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
    long perEvent = elapsedMillis / count;

    String report =
        "ingested=%d elapsedMs=%d avgMsPerEvent=%d%n".formatted(count, elapsedMillis, perEvent);
    Path out = Path.of("target", "perf-smoke.txt");
    Files.createDirectories(out.getParent());
    Files.writeString(out, report);

    assertThat(elapsedMillis).as("ingestion throughput within baseline").isLessThan(60_000);
  }
}
