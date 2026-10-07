package com.park.eventcenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Validates the published OpenAPI contract: it is well-formed, covers every capability and every
 * operation declares responses. The front-end and external clients are expected to integrate
 * against this same contract.
 */
class OpenApiContractTest {

  private static final List<String> REQUIRED_PATHS =
      List.of(
          "/api/auth/login",
          "/api/ingest/events",
          "/api/events",
          "/api/events/{eventId}",
          "/api/events/{eventId}/traces",
          "/api/events/{eventId}/dispatch",
          "/api/events/{eventId}/redeliver",
          "/api/event-types",
          "/api/event-types/{type}",
          "/api/sources",
          "/api/subscriptions",
          "/api/subscriptions/{id}",
          "/api/deliveries",
          "/api/deliveries/dead-letters",
          "/api/notification-channels",
          "/api/notification-templates",
          "/api/notifications",
          "/api/dashboard/statistics",
          "/api/dashboard/situation",
          "/api/dashboard/stream");

  @SuppressWarnings("unchecked")
  @Test
  void contractIsValidAndCoversAllCapabilities() throws Exception {
    Map<String, Object> document;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("openapi/openapi.yaml")) {
      assertThat(in).as("openapi/openapi.yaml must be on the classpath").isNotNull();
      document = new Yaml().load(in);
    }

    assertThat(document.get("openapi")).isEqualTo("3.0.3");
    assertThat(document.get("info")).isInstanceOf(Map.class);

    Map<String, Object> paths = (Map<String, Object>) document.get("paths");
    assertThat(paths).containsKeys(REQUIRED_PATHS.toArray(new String[0]));

    for (String path : REQUIRED_PATHS) {
      Map<String, Object> operations = (Map<String, Object>) paths.get(path);
      assertThat(operations).as("operations for %s", path).isNotEmpty();
      operations.entrySet().stream()
          .filter(
              entry -> List.of("get", "post", "put", "delete", "patch").contains(entry.getKey()))
          .forEach(
              entry -> {
                Map<String, Object> operation = (Map<String, Object>) entry.getValue();
                assertThat(operation)
                    .as("%s %s must declare responses", entry.getKey(), path)
                    .containsKey("responses");
              });
    }

    Map<String, Object> components = (Map<String, Object>) document.get("components");
    Map<String, Object> schemes = (Map<String, Object>) components.get("securitySchemes");
    assertThat(schemes).containsKey("bearerAuth");
  }
}
