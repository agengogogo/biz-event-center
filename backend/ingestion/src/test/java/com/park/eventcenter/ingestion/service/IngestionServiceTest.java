package com.park.eventcenter.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.park.eventcenter.common.bus.EventBus;
import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.common.idempotency.IdempotencyStore;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.ingestion.adapter.SourceAdapterRegistry;
import com.park.eventcenter.ingestion.dto.CreateEventRequest;
import com.park.eventcenter.ingestion.dto.IngestResponse;
import com.park.eventcenter.ingestion.entity.SourceEntity;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import com.park.eventcenter.storequery.service.AuditService;
import com.park.eventcenter.storequery.service.EventStoreService;
import com.park.eventcenter.storequery.service.EventTraceService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies dedup, admin create validation and that accepted events enter the pipeline. */
class IngestionServiceTest {

  private SourceService sourceService;
  private SourceAdapterRegistry adapterRegistry;
  private EventStoreService eventStoreService;
  private EventBus eventBus;
  private EventTypeService eventTypeService;
  private IngestionService service;

  @BeforeEach
  void setUp() {
    ParkContext.set("PARK-A");
    sourceService = mock(SourceService.class);
    adapterRegistry = mock(SourceAdapterRegistry.class);
    eventStoreService = mock(EventStoreService.class);
    eventBus = mock(EventBus.class);
    eventTypeService = mock(EventTypeService.class);
    when(eventTypeService.validatePayload(any(), any()))
        .thenReturn(PayloadValidationResult.validated());
    service =
        new IngestionService(
            sourceService,
            adapterRegistry,
            eventStoreService,
            mock(EventTraceService.class),
            mock(AuditService.class),
            eventBus,
            new InMemoryStore(),
            eventTypeService);
  }

  @AfterEach
  void tearDown() {
    ParkContext.clear();
  }

  @Test
  void duplicateEventIsOnlyAcceptedOnce() {
    CanonicalEvent event = event("evt-dup");
    SourceEntity source = source();
    when(sourceService.authenticate("src-1", "secret")).thenReturn(source);
    when(adapterRegistry.adapt(any(), any())).thenReturn(event);

    IngestResponse first = service.ingest("src-1", "secret", Map.of());
    IngestResponse second = service.ingest("src-1", "secret", Map.of());

    assertThat(first.accepted()).isTrue();
    assertThat(first.duplicate()).isFalse();
    assertThat(second.accepted()).isFalse();
    assertThat(second.duplicate()).isTrue();
  }

  @Test
  void adminCreateRequiresEventTypeAndOccurredAt() {
    assertThatThrownBy(
            () ->
                service.create(
                    new CreateEventRequest(null, null, null, null, null, null, Map.of(), null),
                    "admin"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("eventType");

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateEventRequest(
                        null, "manual.test", null, null, null, null, Map.of(), null),
                    "admin"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("occurredAt");
  }

  @Test
  void adminCreateEntersUnifiedPipeline() {
    IngestResponse response =
        service.create(
            new CreateEventRequest(
                "evt-manual",
                "manual.test",
                Instant.parse("2026-01-02T03:04:05Z"),
                EventSeverity.MAJOR,
                "Gate 1",
                SourceType.MANUAL,
                Map.of("k", "v"),
                null),
            "admin");

    assertThat(response.accepted()).isTrue();
    verify(eventStoreService).persist(any(CanonicalEvent.class), any(), any(), any());
    verify(eventBus).publish(any(CanonicalEvent.class));
  }

  private SourceEntity source() {
    SourceEntity source = new SourceEntity();
    source.setId("src-1");
    source.setSourceType(SourceType.ACCESS);
    source.setParkId("PARK-A");
    source.setEnabled(true);
    return source;
  }

  private CanonicalEvent event(String eventId) {
    return new CanonicalEvent(
        eventId,
        "access.denied",
        "src-1",
        SourceType.ACCESS,
        Instant.parse("2026-01-02T03:04:05Z"),
        EventSeverity.MAJOR,
        "PARK-A",
        "Gate 1",
        Map.of(),
        null,
        null);
  }

  private static final class InMemoryStore implements IdempotencyStore {
    private final Set<String> keys = new HashSet<>();

    @Override
    public boolean markIfAbsent(String key, Duration ttl) {
      return keys.add(key);
    }

    @Override
    public boolean exists(String key) {
      return keys.contains(key);
    }

    @Override
    public void mark(String key, Duration ttl) {
      keys.add(key);
    }
  }
}
