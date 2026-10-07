package com.park.eventcenter.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import com.park.eventcenter.subscription.dto.TransformRule;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies declarative payload transformation before delivery. */
class PayloadTransformerTest {

  private final PayloadTransformer transformer = new PayloadTransformer(new ObjectMapper());

  private CanonicalEvent event() {
    return new CanonicalEvent(
        "evt-1",
        "energy.alarm",
        "meter-9",
        SourceType.ENERGY,
        Instant.parse("2026-01-02T03:04:05Z"),
        EventSeverity.MAJOR,
        "PARK-A",
        "Gate 1",
        Map.of("title", "Overload", "value", 42),
        null,
        "trace-1");
  }

  @Test
  void appliesDeclarativeFieldMapping() {
    List<TransformRule> rules =
        List.of(
            new TransformRule("payload.title", "alert.title", null),
            new TransformRule("eventType", "alert.type", null),
            new TransformRule("payload.missing", "alert.fallback", "N/A"));

    CanonicalEvent transformed = transformer.apply(event(), rules);

    assertThat(transformed.payload()).containsKey("alert");
    @SuppressWarnings("unchecked")
    Map<String, Object> alert = (Map<String, Object>) transformed.payload().get("alert");
    assertThat(alert)
        .containsEntry("title", "Overload")
        .containsEntry("type", "energy.alarm")
        .containsEntry("fallback", "N/A");
    assertThat(transformed.eventId()).isEqualTo("evt-1");
  }

  @Test
  void returnsOriginalEventWhenNoRules() {
    CanonicalEvent event = event();
    CanonicalEvent transformed = transformer.apply(event, List.of());
    assertThat(transformed).isSameAs(event);
  }

  @Test
  void roundTripsRulesThroughJson() {
    List<TransformRule> rules = List.of(new TransformRule("eventId", "id", null));
    String json = transformer.serialize(rules);
    assertThat(transformer.parse(json)).hasSize(1);
    assertThat(transformer.parse(json).get(0).to()).isEqualTo("id");
  }
}
