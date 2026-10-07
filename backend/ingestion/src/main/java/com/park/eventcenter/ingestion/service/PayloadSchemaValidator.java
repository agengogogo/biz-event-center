package com.park.eventcenter.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.park.eventcenter.common.error.BadRequestException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Wraps a JSON Schema (draft 2020-12) validator for business event payloads.
 *
 * <p>Compiled schemas are cached by their text. Invalid schemas fail fast when a definition is
 * saved, while {@link #validate} returns a message instead of throwing so ingestion stays
 * non-blocking.
 */
@Component
public class PayloadSchemaValidator {

  private final ObjectMapper objectMapper;
  private final JsonSchemaFactory factory =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  private final Map<String, JsonSchema> cache = new ConcurrentHashMap<>();

  public PayloadSchemaValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Verifies the schema text is usable; throws when the definition is malformed. */
  public void assertValid(String schema) {
    if (schema == null || schema.isBlank()) {
      return;
    }
    try {
      factory.getSchema(schema);
    } catch (RuntimeException ex) {
      throw new BadRequestException("Invalid payload schema: " + ex.getMessage());
    }
  }

  /** Returns {@code null} when the payload conforms, otherwise a human-readable error message. */
  public String validate(String schema, Map<String, Object> payload) {
    if (schema == null || schema.isBlank()) {
      return null;
    }
    JsonSchema jsonSchema;
    try {
      jsonSchema = cache.computeIfAbsent(schema, factory::getSchema);
    } catch (RuntimeException ex) {
      return "SCHEMA_INVALID: " + ex.getMessage();
    }
    JsonNode node = objectMapper.valueToTree(payload == null ? Map.of() : payload);
    Set<ValidationMessage> errors = jsonSchema.validate(node);
    if (errors.isEmpty()) {
      return null;
    }
    return errors.iterator().next().getMessage();
  }
}
