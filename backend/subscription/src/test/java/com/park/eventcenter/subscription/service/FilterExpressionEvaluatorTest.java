package com.park.eventcenter.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies filter expression matching and rejection of over-privileged expressions. */
class FilterExpressionEvaluatorTest {

  private final FilterExpressionEvaluator evaluator = new FilterExpressionEvaluator();

  private CanonicalEvent event() {
    return new CanonicalEvent(
        "evt-1",
        "access.denied",
        "gate-01",
        SourceType.ACCESS,
        Instant.parse("2026-01-02T03:04:05Z"),
        EventSeverity.CRITICAL,
        "PARK-A",
        "Gate 1",
        Map.of("attempts", 3),
        null,
        "trace-1");
  }

  @Test
  void matchesOnEnvelopeField() {
    assertThat(evaluator.matches("eventType == 'access.denied'", event())).isTrue();
    assertThat(evaluator.matches("eventType == 'access.granted'", event())).isFalse();
  }

  @Test
  void matchesUsingBuiltInFunctions() {
    assertThat(evaluator.matches("contains(location, 'Gate')", event())).isTrue();
    assertThat(evaluator.matches("severity == 'CRITICAL' and has('traceId')", event())).isTrue();
    assertThat(evaluator.matches("payload.attempts > 5", event())).isFalse();
  }

  @Test
  void blankExpressionMatchesEverything() {
    assertThat(evaluator.matches(null, event())).isTrue();
    assertThat(evaluator.matches("  ", event())).isTrue();
  }

  @Test
  void rejectsTypeReferenceExpression() {
    assertThatThrownBy(() -> evaluator.validate("T(java.lang.Runtime).getRuntime().exec('x')"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("forbidden");
  }

  @Test
  void rejectsClassAccessExpression() {
    assertThatThrownBy(() -> evaluator.validate("getClass().forName('java.lang.Runtime')"))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void rejectsUnsupportedCharacters() {
    assertThatThrownBy(() -> evaluator.validate("eventType == 'x'; import java.io.File"))
        .isInstanceOf(BadRequestException.class);
  }
}
