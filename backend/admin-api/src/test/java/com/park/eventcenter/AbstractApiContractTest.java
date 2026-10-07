package com.park.eventcenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.dispatch.repository.DeliveryRepository;
import com.park.eventcenter.notification.repository.NotificationDeliveryRepository;
import com.park.eventcenter.storequery.repository.EventRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Client-independent API contract scenarios shared by the profile-specific contract tests.
 *
 * <p>The same scenario set is executed under the {@code local} profile (embedded SQLite, see {@link
 * LocalProfileContractTest}) and under the production profile (PostgreSQL/Redis/Kafka, see {@link
 * EventCenterIntegrationTest}, gated by {@code BEC_IT=true}). Keeping the scenarios here is what
 * proves the observable HTTP behaviour is identical across the pluggable infrastructure backends.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractApiContractTest {

  protected static final List<String> WEBHOOK_BODIES = new CopyOnWriteArrayList<>();

  private static HttpServer webhook;

  @BeforeAll
  static void startWebhook() throws Exception {
    if (webhook != null) {
      return;
    }
    webhook = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    webhook.createContext(
        "/hook",
        exchange -> {
          byte[] body = exchange.getRequestBody().readAllBytes();
          WEBHOOK_BODIES.add(new String(body, StandardCharsets.UTF_8));
          byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(response);
          }
        });
    webhook.start();
  }

  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper mapper;
  @Autowired protected EventRepository eventRepository;
  @Autowired protected DeliveryRepository deliveryRepository;
  @Autowired protected NotificationDeliveryRepository notificationDeliveryRepository;

  @Test
  void sourceAuthValidationAndDedup() throws Exception {
    String token = login("admin", "admin123");
    JsonNode source = createSource(token, "gate-sensor");
    String sourceId = source.get("id").asText();
    String secret = source.get("secret").asText();

    String eventId = "it-" + UUID.randomUUID();
    JsonNode accepted = ingest(sourceId, secret, body(eventId, "access.denied"));
    assertThat(accepted.get("accepted").asBoolean()).isTrue();
    assertThat(accepted.get("duplicate").asBoolean()).isFalse();

    JsonNode duplicate = ingest(sourceId, secret, body(eventId, "access.denied"));
    assertThat(duplicate.get("accepted").asBoolean()).isFalse();
    assertThat(duplicate.get("duplicate").asBoolean()).isTrue();

    MvcResult badSecret =
        mockMvc
            .perform(
                post("/api/ingest/events")
                    .header("X-Source-Id", sourceId)
                    .header("X-Source-Secret", "wrong")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(body("it-x", "access.denied"))))
            .andReturn();
    assertThat(badSecret.getResponse().getStatus()).isEqualTo(401);

    MvcResult missingOccurredAt =
        mockMvc
            .perform(
                post("/api/ingest/events")
                    .header("X-Source-Id", sourceId)
                    .header("X-Source-Secret", secret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"eventType\":\"access.denied\"}"))
            .andReturn();
    assertThat(missingOccurredAt.getResponse().getStatus()).isEqualTo(400);

    MvcResult unknownSource =
        mockMvc
            .perform(
                post("/api/ingest/events")
                    .header("X-Source-Id", "unknown")
                    .header("X-Source-Secret", "nope")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(body("it-y", "access.denied"))))
            .andReturn();
    assertThat(unknownSource.getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void persistenceQueryAndTrace() throws Exception {
    String token = login("admin", "admin123");
    JsonNode source = createSource(token, "query-sensor");
    String eventId = "it-" + UUID.randomUUID();
    ingest(source.get("id").asText(), source.get("secret").asText(), body(eventId, "energy.alarm"));

    JsonNode event = getJson(token, "/api/events/" + eventId);
    assertThat(event.get("eventId").asText()).isEqualTo(eventId);
    assertThat(event.get("parkId").asText()).isEqualTo("PARK-A");

    JsonNode page = getJson(token, "/api/events?type=energy.alarm&size=50");
    assertThat(page.get("total").asLong()).isGreaterThanOrEqualTo(1);

    JsonNode traces = getJson(token, "/api/events/" + eventId + "/traces");
    assertThat(traces.isArray()).isTrue();
    assertThat(traces.size()).isGreaterThanOrEqualTo(3);
  }

  @Test
  void acceptedEventIsProcessed() throws Exception {
    String token = login("admin", "admin123");
    JsonNode source = createSource(token, "pipeline-sensor");
    String eventId = "it-" + UUID.randomUUID();
    ingest(
        source.get("id").asText(),
        source.get("secret").asText(),
        body(eventId, "metrics.heartbeat"));

    boolean processed =
        await(
            Duration.ofSeconds(20),
            () ->
                eventRepository
                    .findByEventIdAndParkId(eventId, "PARK-A")
                    .map(e -> !"RECEIVED".equals(e.getStatus().name()))
                    .orElse(false));
    assertThat(processed).as("event consumed and dispatched by the configured event bus").isTrue();
  }

  @Test
  void dispatchToSubscriptionAndDeadLetter() throws Exception {
    String token = login("admin", "admin123");
    createSubscription(token, "webhook-sub", "iot.*", "WEBHOOK", webhookUrl(), null, null, 0);

    JsonNode source = createSource(token, "iot-sensor");
    String eventId = "it-" + UUID.randomUUID();
    ingest(
        source.get("id").asText(), source.get("secret").asText(), body(eventId, "iot.temperature"));

    boolean delivered =
        await(
            Duration.ofSeconds(20),
            () ->
                deliveryRepository
                    .findByEventIdAndParkIdOrderByCreatedAtDesc(eventId, "PARK-A")
                    .stream()
                    .anyMatch(d -> "DELIVERED".equals(d.getStatus())));
    assertThat(delivered).as("webhook delivery recorded").isTrue();
    assertThat(await(Duration.ofSeconds(10), () -> !WEBHOOK_BODIES.isEmpty())).isTrue();

    createSubscription(
        token, "dlq-sub", "dlq.*", "WEBHOOK", "http://127.0.0.1:1/dead", null, null, 1);
    String dlqEventId = "it-" + UUID.randomUUID();
    ingest(
        source.get("id").asText(), source.get("secret").asText(), body(dlqEventId, "dlq.failure"));

    boolean deadLettered =
        await(
            Duration.ofSeconds(30),
            () ->
                deliveryRepository
                    .findByEventIdAndParkIdOrderByCreatedAtDesc(dlqEventId, "PARK-A")
                    .stream()
                    .anyMatch(d -> d.isDeadLetter()));
    assertThat(deadLettered).as("failed delivery moved to dead letter").isTrue();

    JsonNode deadLetters = getJson(token, "/api/deliveries/dead-letters");
    assertThat(deadLetters.isArray()).isTrue();
  }

  @Test
  void dashboardStatisticsAndSituation() throws Exception {
    String token = login("admin", "admin123");
    JsonNode source = createSource(token, "dash-sensor");
    String eventId = "it-" + UUID.randomUUID();
    ingest(
        source.get("id").asText(),
        source.get("secret").asText(),
        body(eventId, "security.intrusion"));

    JsonNode statistics = getJson(token, "/api/dashboard/statistics?dimension=SEVERITY");
    assertThat(statistics.get("total").asLong()).isGreaterThanOrEqualTo(1);
    assertThat(statistics.get("points").isArray()).isTrue();

    JsonNode situation = getJson(token, "/api/dashboard/situation");
    assertThat(situation.has("activeCount")).isTrue();
    assertThat(situation.get("activeAlerts").isArray()).isTrue();
  }

  @Test
  void multiParkIsolationRejectsCrossParkAccess() throws Exception {
    String adminToken = login("admin", "admin123");
    String opsToken = login("ops", "ops123");

    JsonNode source = createSource(opsToken, "ops-sensor");
    String eventId = "it-" + UUID.randomUUID();
    ingest(
        source.get("id").asText(), source.get("secret").asText(), body(eventId, "access.granted"));

    MvcResult crossPark =
        mockMvc
            .perform(get("/api/events/" + eventId).header("Authorization", bearer(adminToken)))
            .andReturn();
    assertThat(crossPark.getResponse().getStatus()).isEqualTo(404);

    JsonNode opsView = getJson(opsToken, "/api/events/" + eventId);
    assertThat(opsView.get("parkId").asText()).isEqualTo("PARK-B");
  }

  @Test
  void eventTypeValidationIsRecordedNonBlocking() throws Exception {
    String token = login("admin", "admin123");

    JsonNode catalog = getJson(token, "/api/event-types");
    assertThat(catalog.isArray()).isTrue();
    assertThat(catalog.size()).as("preset event type catalog").isGreaterThanOrEqualTo(7);

    JsonNode source = createSource(token, "validation-sensor");
    String sourceId = source.get("id").asText();
    String secret = source.get("secret").asText();

    String conformingId = "it-" + UUID.randomUUID();
    ingest(sourceId, secret, event("iot.temperature", conformingId, Map.of("value", 21.5)));
    JsonNode conforming = getJson(token, "/api/events/" + conformingId);
    assertThat(conforming.get("eventTypeValidation").asText()).isEqualTo("VALIDATED");

    String mismatchId = "it-" + UUID.randomUUID();
    JsonNode accepted =
        ingest(sourceId, secret, event("access.denied", mismatchId, Map.of("gateId", "G1")));
    assertThat(accepted.get("accepted").asBoolean()).isTrue();
    JsonNode mismatch = getJson(token, "/api/events/" + mismatchId);
    assertThat(mismatch.get("eventTypeValidation").asText()).isEqualTo("UNVALIDATED");
    assertThat(mismatch.get("validationReason").asText()).contains("SCHEMA_MISMATCH");

    String unknownId = "it-" + UUID.randomUUID();
    ingest(sourceId, secret, event("custom.unregistered", unknownId, Map.of("anything", 1)));
    JsonNode unknown = getJson(token, "/api/events/" + unknownId);
    assertThat(unknown.get("eventTypeValidation").asText()).isEqualTo("UNVALIDATED");
    assertThat(unknown.get("validationReason").asText()).contains("UNKNOWN_TYPE");
  }

  private Map<String, Object> event(String eventType, String eventId, Map<String, Object> payload) {
    return Map.of(
        "eventId",
        eventId,
        "eventType",
        eventType,
        "occurredAt",
        Instant.now().toString(),
        "severity",
        "MAJOR",
        "payload",
        payload);
  }

  protected String webhookUrl() {
    return "http://127.0.0.1:" + webhook.getAddress().getPort() + "/hook";
  }

  protected String login(String username, String password) throws Exception {
    JsonNode response =
        postJson(null, "/api/auth/login", Map.of("username", username, "password", password));
    return response.get("token").asText();
  }

  protected JsonNode createSource(String token, String name) throws Exception {
    return postJson(
        token, "/api/sources", Map.of("name", name + "-" + UUID.randomUUID(), "sourceType", "IOT"));
  }

  protected void createSubscription(
      String token,
      String name,
      String eventType,
      String targetType,
      String targetUrl,
      String channelId,
      String templateId,
      int maxRetries)
      throws Exception {
    Map<String, Object> body = new java.util.HashMap<>();
    body.put("name", name);
    body.put("eventType", eventType);
    body.put("targetType", targetType);
    body.put("maxRetries", maxRetries);
    body.put("backoffMillis", 0);
    if (targetUrl != null) {
      body.put("targetUrl", targetUrl);
    }
    if (channelId != null) {
      body.put("notificationChannelId", channelId);
    }
    if (templateId != null) {
      body.put("templateId", templateId);
    }
    postJson(token, "/api/subscriptions", body);
  }

  protected JsonNode ingest(String sourceId, String secret, Map<String, Object> body)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/ingest/events")
                    .header("X-Source-Id", sourceId)
                    .header("X-Source-Secret", secret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(body)))
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  protected Map<String, Object> body(String eventId, String eventType) {
    return Map.of(
        "eventId",
        eventId,
        "eventType",
        eventType,
        "occurredAt",
        Instant.now().toString(),
        "severity",
        "MAJOR",
        "location",
        "Gate 1",
        "payload",
        Map.of("value", 1));
  }

  protected JsonNode postJson(String token, String url, Object body) throws Exception {
    var request =
        post(url).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
    if (token != null) {
      request.header("Authorization", bearer(token));
    }
    MvcResult result = mockMvc.perform(request).andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  protected JsonNode getJson(String token, String url) throws Exception {
    MvcResult result = mockMvc.perform(get(url).header("Authorization", bearer(token))).andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  protected String bearer(String token) {
    return "Bearer " + token;
  }

  protected boolean await(Duration timeout, BooleanSupplier condition) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeout.toMillis();
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return true;
      }
      Thread.sleep(200);
    }
    return condition.getAsBoolean();
  }
}
