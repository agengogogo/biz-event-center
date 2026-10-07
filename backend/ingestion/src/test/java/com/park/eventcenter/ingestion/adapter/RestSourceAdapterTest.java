package com.park.eventcenter.ingestion.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.ingestion.entity.SourceEntity;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies heterogeneous payload standardization and required-field validation. */
class RestSourceAdapterTest {

  private final RestSourceAdapter adapter = new RestSourceAdapter();

  private SourceEntity source() {
    SourceEntity source = new SourceEntity();
    source.setId("src-1");
    source.setName("Gate sensor");
    source.setSourceType(SourceType.IOT);
    source.setParkId("PARK-A");
    return source;
  }

  @Test
  void mapsHeterogeneousFieldsIntoSchemaAndKeepsRawPayload() {
    Map<String, Object> raw = new HashMap<>();
    raw.put("type", "iot.temperature");
    raw.put("timestamp", "2026-01-02T03:04:05Z");
    raw.put("level", "critical");
    raw.put("place", "Block A");
    raw.put("data", Map.of("value", 21.5));

    CanonicalEvent event = adapter.adapt(source(), raw);

    assertThat(event.eventType()).isEqualTo("iot.temperature");
    assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"));
    assertThat(event.severity()).isEqualTo(EventSeverity.CRITICAL);
    assertThat(event.location()).isEqualTo("Block A");
    assertThat(event.sourceType()).isEqualTo(SourceType.IOT);
    assertThat(event.parkId()).isEqualTo("PARK-A");
    assertThat(event.payload()).containsEntry("value", 21.5);
    assertThat(event.rawPayload()).containsKeys("type", "timestamp", "level", "place", "data");
    assertThat(event.eventId()).isNotBlank();
  }

  @Test
  void defaultsSeverityWhenAbsent() {
    Map<String, Object> raw = new HashMap<>();
    raw.put("eventType", "iot.ping");
    raw.put("occurredAt", 1_700_000_000_000L);

    CanonicalEvent event = adapter.adapt(source(), raw);

    assertThat(event.severity()).isEqualTo(EventSeverity.WARNING);
    assertThat(event.occurredAt()).isEqualTo(Instant.ofEpochMilli(1_700_000_000_000L));
  }

  @Test
  void rejectsMissingEventType() {
    assertThatThrownBy(() -> adapter.adapt(source(), Map.of("occurredAt", "2026-01-02T03:04:05Z")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("eventType");
  }

  @Test
  void rejectsMissingOccurredAt() {
    assertThatThrownBy(() -> adapter.adapt(source(), Map.of("eventType", "iot.ping")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("occurredAt");
  }
}
