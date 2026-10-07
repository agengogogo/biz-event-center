package com.park.eventcenter.storequery.service;

import com.park.eventcenter.common.error.ConflictException;
import com.park.eventcenter.common.error.NotFoundException;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventStatus;
import com.park.eventcenter.model.PayloadValidationStatus;
import com.park.eventcenter.storequery.dto.EventView;
import com.park.eventcenter.storequery.entity.EventEntity;
import com.park.eventcenter.storequery.repository.EventRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists canonical events and supports unique-identifier retrieval and status updates. */
@Service
public class EventStoreService {

  private final EventRepository repository;
  private final EventMapper mapper;

  public EventStoreService(EventRepository repository, EventMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional
  public EventView persist(
      CanonicalEvent event,
      EventStatus status,
      PayloadValidationStatus eventTypeValidation,
      String validationReason) {
    EventEntity entity = repository.findById(event.eventId()).orElse(null);
    Instant now = Instant.now();
    if (entity == null) {
      entity = new EventEntity(event.eventId(), event.parkId());
      entity.setReceivedAt(now);
    } else if (!entity.getParkId().equals(event.parkId())) {
      throw new ConflictException("Event id already used by another park");
    }
    entity.setEventType(event.eventType());
    entity.setSource(event.source());
    entity.setSourceType(event.sourceType());
    entity.setOccurredAt(event.occurredAt());
    entity.setSeverity(event.severity());
    entity.setLocation(event.location());
    entity.setPayload(mapper.writeMap(event.payload()));
    entity.setRawPayload(mapper.writeMap(event.rawPayload()));
    entity.setTraceId(event.traceId());
    entity.setStatus(status);
    entity.setEventTypeValidation(eventTypeValidation);
    entity.setValidationReason(validationReason);
    entity.setUpdatedAt(now);
    return mapper.toView(repository.save(entity));
  }

  @Transactional(readOnly = true)
  public Optional<EventView> findById(String eventId) {
    return findByPark(eventId, ParkContext.require());
  }

  @Transactional(readOnly = true)
  public Optional<EventView> findByPark(String eventId, String parkId) {
    return repository.findByEventIdAndParkId(eventId, parkId).map(mapper::toView);
  }

  @Transactional(readOnly = true)
  public EventView getById(String eventId, String parkId) {
    return findByPark(eventId, parkId)
        .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
  }

  @Transactional(readOnly = true)
  public EventEntity requireEntity(String eventId, String parkId) {
    return repository
        .findByEventIdAndParkId(eventId, parkId)
        .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
  }

  @Transactional(readOnly = true)
  public Optional<CanonicalEvent> findCanonical(String eventId, String parkId) {
    return repository.findByEventIdAndParkId(eventId, parkId).map(mapper::toCanonical);
  }

  @Transactional
  public void updateStatus(String eventId, String parkId, EventStatus status) {
    EventEntity entity = requireEntity(eventId, parkId);
    entity.setStatus(status);
    entity.setUpdatedAt(Instant.now());
    repository.save(entity);
  }
}
