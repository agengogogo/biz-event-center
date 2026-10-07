package com.park.eventcenter.ingestion.service;

import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.common.error.ConflictException;
import com.park.eventcenter.common.error.NotFoundException;
import com.park.eventcenter.ingestion.dto.EventTypeRequest;
import com.park.eventcenter.ingestion.dto.EventTypeView;
import com.park.eventcenter.ingestion.entity.EventTypeEntity;
import com.park.eventcenter.ingestion.repository.EventTypeRepository;
import com.park.eventcenter.model.EventSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages business event type definitions and validates event payloads against their schemas. */
@Service
public class EventTypeService {

  private final EventTypeRepository repository;
  private final PayloadSchemaValidator schemaValidator;

  public EventTypeService(EventTypeRepository repository, PayloadSchemaValidator schemaValidator) {
    this.repository = repository;
    this.schemaValidator = schemaValidator;
  }

  @Transactional(readOnly = true)
  public List<EventTypeView> list(Boolean enabledOnly) {
    List<EventTypeEntity> entities =
        Boolean.TRUE.equals(enabledOnly)
            ? repository.findByEnabledTrueOrderByTypeAsc()
            : repository.findAllByOrderByTypeAsc();
    return entities.stream().map(EventTypeService::toView).toList();
  }

  @Transactional(readOnly = true)
  public EventTypeView get(String type) {
    return toView(require(type));
  }

  @Transactional
  public EventTypeView create(EventTypeRequest request) {
    String type = requireType(request.type());
    if (repository.findById(type).isPresent()) {
      throw new ConflictException("Event type already exists: " + type);
    }
    schemaValidator.assertValid(request.payloadSchema());
    EventTypeEntity entity = new EventTypeEntity();
    entity.setType(type);
    entity.setVersion(1);
    entity.setCreatedAt(Instant.now());
    apply(entity, request);
    return toView(repository.save(entity));
  }

  @Transactional
  public EventTypeView update(String type, EventTypeRequest request) {
    EventTypeEntity entity = require(type);
    schemaValidator.assertValid(request.payloadSchema());
    apply(entity, request);
    entity.setVersion(entity.getVersion() + 1);
    return toView(repository.save(entity));
  }

  @Transactional
  public EventTypeView setEnabled(String type, boolean enabled) {
    EventTypeEntity entity = require(type);
    entity.setEnabled(enabled);
    entity.setUpdatedAt(Instant.now());
    return toView(repository.save(entity));
  }

  /**
   * Resolves the business event type and validates the payload against its schema. Never throws for
   * unknown, disabled or non-conforming types: the outcome is recorded for observability instead.
   */
  @Transactional(readOnly = true)
  public PayloadValidationResult validatePayload(String eventType, Map<String, Object> payload) {
    Optional<EventTypeEntity> found = repository.findById(eventType);
    if (found.isEmpty()) {
      return PayloadValidationResult.unvalidated("UNKNOWN_TYPE: " + eventType);
    }
    EventTypeEntity definition = found.get();
    if (!definition.isEnabled()) {
      return PayloadValidationResult.unvalidated("DISABLED_TYPE: " + eventType);
    }
    String error = schemaValidator.validate(definition.getPayloadSchema(), payload);
    if (error == null) {
      return PayloadValidationResult.validated();
    }
    if (error.startsWith("SCHEMA_INVALID")) {
      return PayloadValidationResult.unvalidated(error);
    }
    return PayloadValidationResult.unvalidated("SCHEMA_MISMATCH: " + error);
  }

  private void apply(EventTypeEntity entity, EventTypeRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new BadRequestException("name is required");
    }
    if (request.sourceCategory() == null) {
      throw new BadRequestException("sourceCategory is required");
    }
    entity.setName(request.name());
    entity.setSourceCategory(request.sourceCategory());
    entity.setDefaultSeverity(
        request.defaultSeverity() == null ? EventSeverity.WARNING : request.defaultSeverity());
    entity.setPayloadSchema(request.payloadSchema());
    entity.setEnabled(request.enabled() == null || request.enabled());
    entity.setUpdatedAt(Instant.now());
  }

  private String requireType(String type) {
    if (type == null || type.isBlank()) {
      throw new BadRequestException("type is required");
    }
    return type.trim();
  }

  private EventTypeEntity require(String type) {
    return repository
        .findById(type)
        .orElseThrow(() -> new NotFoundException("Event type not found: " + type));
  }

  private static EventTypeView toView(EventTypeEntity entity) {
    return new EventTypeView(
        entity.getType(),
        entity.getName(),
        entity.getSourceCategory(),
        entity.getDefaultSeverity(),
        entity.getPayloadSchema(),
        entity.isEnabled(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
