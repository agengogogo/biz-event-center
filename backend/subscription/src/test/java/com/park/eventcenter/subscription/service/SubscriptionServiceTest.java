package com.park.eventcenter.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.subscription.dto.SubscriptionRequest;
import com.park.eventcenter.subscription.dto.SubscriptionView;
import com.park.eventcenter.subscription.entity.SubscriptionEntity;
import com.park.eventcenter.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies subscription lifecycle, park scoping and topic matching. */
class SubscriptionServiceTest {

  private SubscriptionRepository repository;
  private SubscriptionService service;

  @BeforeEach
  void setUp() {
    ParkContext.set("PARK-A");
    repository = mock(SubscriptionRepository.class);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service =
        new SubscriptionService(
            repository,
            new FilterExpressionEvaluator(),
            new PayloadTransformer(new ObjectMapper()));
  }

  @AfterEach
  void tearDown() {
    ParkContext.clear();
  }

  @Test
  void createIsParkScopedAndEnabledByDefault() {
    SubscriptionView view = service.create(request("access.*", null));

    assertThat(view.parkId()).isEqualTo("PARK-A");
    assertThat(view.enabled()).isTrue();
    assertThat(view.id()).isNotBlank();
  }

  @Test
  void createRejectsOverPrivilegedFilterExpression() {
    assertThatThrownBy(() -> service.create(request("*", "T(java.lang.Runtime).getRuntime()")))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void enableAndDisableToggleState() {
    SubscriptionEntity entity = entity("sub-1");
    when(repository.findByIdAndParkId("sub-1", "PARK-A")).thenReturn(Optional.of(entity));

    assertThat(service.setEnabled("sub-1", false).enabled()).isFalse();
    assertThat(service.setEnabled("sub-1", true).enabled()).isTrue();
  }

  @Test
  void deleteRemovesSubscription() {
    SubscriptionEntity entity = entity("sub-1");
    when(repository.findByIdAndParkId("sub-1", "PARK-A")).thenReturn(Optional.of(entity));

    service.delete("sub-1");

    verify(repository).delete(entity);
  }

  @Test
  void matchesTopicSupportsExactWildcardAndPrefix() {
    assertThat(service.matchesTopic("*", "anything")).isTrue();
    assertThat(service.matchesTopic("access.*", "access.denied")).isTrue();
    assertThat(service.matchesTopic("access.denied", "access.denied")).isTrue();
    assertThat(service.matchesTopic("access.denied", "access.granted")).isFalse();
  }

  private SubscriptionRequest request(String eventType, String filter) {
    return new SubscriptionRequest(
        "sub",
        eventType,
        true,
        filter,
        List.of(),
        "WEBHOOK",
        "http://localhost/x",
        null,
        null,
        false,
        3,
        1000L);
  }

  private SubscriptionEntity entity(String id) {
    SubscriptionEntity entity = new SubscriptionEntity();
    entity.setId(id);
    entity.setParkId("PARK-A");
    entity.setEventType("access.*");
    entity.setEnabled(true);
    entity.setTargetType("WEBHOOK");
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    return entity;
  }
}
