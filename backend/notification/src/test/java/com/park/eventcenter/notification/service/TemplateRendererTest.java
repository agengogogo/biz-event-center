package com.park.eventcenter.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies template rendering that references event fields. */
class TemplateRendererTest {

  private final TemplateRenderer renderer = new TemplateRenderer();

  private CanonicalEvent event() {
    return new CanonicalEvent(
        "evt-1",
        "energy.alarm",
        "meter-9",
        SourceType.ENERGY,
        Instant.parse("2026-01-02T03:04:05Z"),
        EventSeverity.CRITICAL,
        "PARK-A",
        "Gate 1",
        Map.of("value", 42),
        null,
        "trace-1");
  }

  @Test
  void rendersEventAndPayloadFields() {
    String content =
        renderer.render(
            "[${severity}] ${eventType} at ${location}, value=${payload.value}, id=${eventId}",
            event());
    assertThat(content).isEqualTo("[CRITICAL] energy.alarm at Gate 1, value=42, id=evt-1");
  }

  @Test
  void rendersMissingFieldsAsEmpty() {
    assertThat(renderer.render("x=${payload.absent}", event())).isEqualTo("x=");
  }

  @Test
  void returnsNullForNullTemplate() {
    assertThat(renderer.render(null, event())).isNull();
  }
}
