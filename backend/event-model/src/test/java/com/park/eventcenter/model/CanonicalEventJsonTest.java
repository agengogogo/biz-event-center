package com.park.eventcenter.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies canonical event field mapping and JSON serialization/deserialization. */
class CanonicalEventJsonTest {

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void roundTripsCanonicalEvent() throws Exception {
    CanonicalEvent event =
        new CanonicalEvent(
            "evt-1",
            "access.denied",
            "gate-01",
            SourceType.ACCESS,
            Instant.parse("2026-01-02T03:04:05Z"),
            EventSeverity.MAJOR,
            "PARK-A",
            "Gate 1",
            Map.of("personId", "p-9", "attempts", 3),
            Map.of("raw", "denied"),
            "trace-1");

    String json = mapper.writeValueAsString(event);
    CanonicalEvent restored = mapper.readValue(json, CanonicalEvent.class);

    assertThat(restored.eventId()).isEqualTo("evt-1");
    assertThat(restored.eventType()).isEqualTo("access.denied");
    assertThat(restored.source()).isEqualTo("gate-01");
    assertThat(restored.sourceType()).isEqualTo(SourceType.ACCESS);
    assertThat(restored.occurredAt()).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"));
    assertThat(restored.severity()).isEqualTo(EventSeverity.MAJOR);
    assertThat(restored.parkId()).isEqualTo("PARK-A");
    assertThat(restored.location()).isEqualTo("Gate 1");
    assertThat(restored.payload()).containsEntry("personId", "p-9").containsEntry("attempts", 3);
    assertThat(restored.rawPayload()).containsEntry("raw", "denied");
    assertThat(restored.traceId()).isEqualTo("trace-1");
  }

  @Test
  void omitsNullOptionalFields() throws Exception {
    CanonicalEvent event =
        new CanonicalEvent(
            "evt-2",
            "iot.temperature",
            "sensor-7",
            SourceType.IOT,
            Instant.parse("2026-01-02T03:04:05Z"),
            EventSeverity.INFO,
            "PARK-A",
            null,
            Map.of("value", 21.5),
            null,
            null);

    JsonNode node = mapper.readTree(mapper.writeValueAsString(event));

    assertThat(node.has("location")).isFalse();
    assertThat(node.has("rawPayload")).isFalse();
    assertThat(node.has("traceId")).isFalse();
    assertThat(node.has("payload")).isTrue();
  }

  @Test
  void rejectsMissingMandatoryFields() {
    assertThatThrownBy(
            () ->
                new CanonicalEvent(
                    "evt-3",
                    null,
                    "source",
                    SourceType.IOT,
                    Instant.now(),
                    EventSeverity.INFO,
                    "PARK-A",
                    null,
                    Map.of(),
                    null,
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("eventType");
  }

  @Test
  void schemaResourceDeclaresRequiredEnvelopeFields() throws Exception {
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream("schema/canonical-event.schema.json")) {
      JsonNode schema = mapper.readTree(in);
      List<String> required =
          mapper.convertValue(
              schema.get("required"),
              mapper.getTypeFactory().constructCollectionType(List.class, String.class));
      assertThat(required)
          .contains(
              "eventId", "eventType", "source", "sourceType", "occurredAt", "severity", "parkId");
      assertThat(schema.at("/properties/payload/type").asText()).isEqualTo("object");
    }
  }
}
