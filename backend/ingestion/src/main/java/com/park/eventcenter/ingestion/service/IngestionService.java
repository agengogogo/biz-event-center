package com.park.eventcenter.ingestion.service;

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
import com.park.eventcenter.model.EventStatus;
import com.park.eventcenter.model.SourceType;
import com.park.eventcenter.model.TraceStage;
import com.park.eventcenter.storequery.service.AuditService;
import com.park.eventcenter.storequery.service.EventStoreService;
import com.park.eventcenter.storequery.service.EventTraceService;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Validates, standardizes, deduplicates and accepts events into the unified pipeline. */
@Service
public class IngestionService {

  private static final Duration DEDUP_TTL = Duration.ofDays(7);

  private final SourceService sourceService;
  private final SourceAdapterRegistry adapterRegistry;
  private final EventStoreService eventStoreService;
  private final EventTraceService traceService;
  private final AuditService auditService;
  private final EventBus eventBus;
  private final IdempotencyStore idempotencyStore;
  private final EventTypeService eventTypeService;

  public IngestionService(
      SourceService sourceService,
      SourceAdapterRegistry adapterRegistry,
      EventStoreService eventStoreService,
      EventTraceService traceService,
      AuditService auditService,
      EventBus eventBus,
      IdempotencyStore idempotencyStore,
      EventTypeService eventTypeService) {
    this.sourceService = sourceService;
    this.adapterRegistry = adapterRegistry;
    this.eventStoreService = eventStoreService;
    this.traceService = traceService;
    this.auditService = auditService;
    this.eventBus = eventBus;
    this.idempotencyStore = idempotencyStore;
    this.eventTypeService = eventTypeService;
  }

  public IngestResponse ingest(String sourceId, String secret, Map<String, Object> raw) {
    SourceEntity source = sourceService.authenticate(sourceId, secret);
    CanonicalEvent event = adapterRegistry.adapt(source, raw);
    return accept(event, source.getId());
  }

  public IngestResponse create(CreateEventRequest request, String actor) {
    String parkId = ParkContext.require();
    if (request.eventType() == null || request.eventType().isBlank()) {
      throw new BadRequestException("eventType is required");
    }
    if (request.occurredAt() == null) {
      throw new BadRequestException("occurredAt is required");
    }
    String eventId =
        request.eventId() == null || request.eventId().isBlank()
            ? UUID.randomUUID().toString()
            : request.eventId();
    SourceType sourceType = request.sourceType() == null ? SourceType.MANUAL : request.sourceType();
    EventSeverity severity =
        request.severity() == null ? EventSeverity.WARNING : request.severity();
    Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
    CanonicalEvent event =
        new CanonicalEvent(
            eventId,
            request.eventType(),
            actor,
            sourceType,
            request.occurredAt(),
            severity,
            parkId,
            request.location(),
            payload,
            payload,
            request.traceId());
    return accept(event, actor);
  }

  private IngestResponse accept(CanonicalEvent event, String actor) {
    traceService.append(
        event.eventId(),
        event.parkId(),
        TraceStage.INGESTED,
        "RECEIVED",
        "source=" + event.source());
    traceService.append(event.eventId(), event.parkId(), TraceStage.VALIDATED, "OK", null);

    boolean first = idempotencyStore.markIfAbsent(dedupKey(event.eventId()), DEDUP_TTL);
    if (!first) {
      traceService.append(
          event.eventId(), event.parkId(), TraceStage.DEDUPLICATED, "DUPLICATE", "ignored repeat");
      return new IngestResponse(
          event.eventId(), event.eventType(), false, true, EventStatus.RECEIVED);
    }

    PayloadValidationResult validation =
        eventTypeService.validatePayload(event.eventType(), event.payload());
    eventStoreService.persist(
        event, EventStatus.RECEIVED, validation.status(), validation.reason());
    traceService.append(
        event.eventId(),
        event.parkId(),
        TraceStage.TYPE_VALIDATED,
        validation.status().name(),
        validation.reason());
    traceService.append(event.eventId(), event.parkId(), TraceStage.PERSISTED, "STORED", null);

    eventBus.publish(event);
    traceService.append(
        event.eventId(), event.parkId(), TraceStage.PUBLISHED, "SENT", "topic=park.events");

    auditService.record(
        actor, "INGEST", "EVENT", event.eventId(), event.parkId(), "type=" + event.eventType());
    return new IngestResponse(
        event.eventId(), event.eventType(), true, false, EventStatus.RECEIVED);
  }

  private String dedupKey(String eventId) {
    return "ingest:event:" + eventId;
  }
}
