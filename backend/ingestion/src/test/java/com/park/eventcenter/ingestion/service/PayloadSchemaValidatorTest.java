package com.park.eventcenter.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.error.BadRequestException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies JSON Schema payload validation and schema meta-validation. */
class PayloadSchemaValidatorTest {

  private static final String SCHEMA =
      "{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"number\"}},\"required\":[\"value\"]}";

  private final PayloadSchemaValidator validator = new PayloadSchemaValidator(new ObjectMapper());

  @Test
  void acceptsConformingPayload() {
    assertThat(validator.validate(SCHEMA, Map.of("value", 21.5))).isNull();
  }

  @Test
  void rejectsNonConformingPayload() {
    assertThat(validator.validate(SCHEMA, Map.of("value", "hot"))).isNotNull();
  }

  @Test
  void rejectsMissingRequiredField() {
    assertThat(validator.validate(SCHEMA, Map.of())).isNotNull();
  }

  @Test
  void assertValidRejectsMalformedSchema() {
    assertThatThrownBy(() -> validator.assertValid("{ not json"))
        .isInstanceOf(BadRequestException.class);
  }
}
