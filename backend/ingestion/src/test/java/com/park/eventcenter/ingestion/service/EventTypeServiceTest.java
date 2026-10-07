package com.park.eventcenter.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.ingestion.dto.EventTypeRequest;
import com.park.eventcenter.ingestion.entity.EventTypeEntity;
import com.park.eventcenter.ingestion.repository.EventTypeRepository;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.PayloadValidationStatus;
import com.park.eventcenter.model.SourceType;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies event type definition management and non-blocking payload validation outcomes. */
class EventTypeServiceTest {

  private static final String SCHEMA =
      "{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"number\"}},\"required\":[\"value\"]}";

  private EventTypeRepository repository;
  private EventTypeService service;

  @BeforeEach
  void setUp() {
    repository = mock(EventTypeRepository.class);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service = new EventTypeService(repository, new PayloadSchemaValidator(new ObjectMapper()));
  }

  @Test
  void createRejectsMalformedSchema() {
    assertThatThrownBy(
            () ->
                service.create(
                    new EventTypeRequest(
                        "custom.type",
                        "自定义",
                        SourceType.IOT,
                        EventSeverity.INFO,
                        "{ not json",
                        true)))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void createPersistsDefinition() {
    when(repository.findById("custom.type")).thenReturn(Optional.empty());

    var view =
        service.create(
            new EventTypeRequest(
                "custom.type", "自定义", SourceType.IOT, EventSeverity.INFO, SCHEMA, true));

    assertThat(view.type()).isEqualTo("custom.type");
    assertThat(view.version()).isEqualTo(1);
    assertThat(view.enabled()).isTrue();
  }

  @Test
  void validatePayloadMarksUnknownType() {
    when(repository.findById("x.unknown")).thenReturn(Optional.empty());

    PayloadValidationResult result = service.validatePayload("x.unknown", Map.of());

    assertThat(result.status()).isEqualTo(PayloadValidationStatus.UNVALIDATED);
    assertThat(result.reason()).contains("UNKNOWN_TYPE");
  }

  @Test
  void validatePayloadMarksDisabledType() {
    when(repository.findById("custom.type")).thenReturn(Optional.of(entity(false)));

    PayloadValidationResult result = service.validatePayload("custom.type", Map.of("value", 1));

    assertThat(result.status()).isEqualTo(PayloadValidationStatus.UNVALIDATED);
    assertThat(result.reason()).contains("DISABLED_TYPE");
  }

  @Test
  void validatePayloadDetectsSchemaMismatch() {
    when(repository.findById("custom.type")).thenReturn(Optional.of(entity(true)));

    PayloadValidationResult result = service.validatePayload("custom.type", Map.of());

    assertThat(result.status()).isEqualTo(PayloadValidationStatus.UNVALIDATED);
    assertThat(result.reason()).contains("SCHEMA_MISMATCH");
  }

  @Test
  void validatePayloadAcceptsConformingPayload() {
    when(repository.findById("custom.type")).thenReturn(Optional.of(entity(true)));

    PayloadValidationResult result = service.validatePayload("custom.type", Map.of("value", 21.5));

    assertThat(result.status()).isEqualTo(PayloadValidationStatus.VALIDATED);
    assertThat(result.reason()).isNull();
  }

  private EventTypeEntity entity(boolean enabled) {
    EventTypeEntity entity = new EventTypeEntity();
    entity.setType("custom.type");
    entity.setName("自定义");
    entity.setSourceCategory(SourceType.IOT);
    entity.setDefaultSeverity(EventSeverity.INFO);
    entity.setPayloadSchema(SCHEMA);
    entity.setEnabled(enabled);
    return entity;
  }
}
