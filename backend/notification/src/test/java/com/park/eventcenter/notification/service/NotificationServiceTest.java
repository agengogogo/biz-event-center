package com.park.eventcenter.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import com.park.eventcenter.notification.channel.DeliveryResult;
import com.park.eventcenter.notification.channel.NotificationChannel;
import com.park.eventcenter.notification.channel.NotificationChannelRegistry;
import com.park.eventcenter.notification.channel.NotificationMessage;
import com.park.eventcenter.notification.dto.DeliveryView;
import com.park.eventcenter.notification.entity.NotificationChannelEntity;
import com.park.eventcenter.notification.entity.NotificationDeliveryEntity;
import com.park.eventcenter.notification.entity.NotificationTemplateEntity;
import com.park.eventcenter.notification.repository.NotificationChannelRepository;
import com.park.eventcenter.notification.repository.NotificationDeliveryRepository;
import com.park.eventcenter.notification.repository.NotificationTemplateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies channel dispatch, template rendering, result recording and manual resend. */
class NotificationServiceTest {

  private final StubChannel stub = new StubChannel();
  private NotificationDeliveryRepository deliveryRepository;
  private NotificationChannelRepository channelRepository;
  private NotificationTemplateRepository templateRepository;
  private NotificationService service;

  @BeforeEach
  void setUp() {
    ParkContext.set("PARK-A");
    stub.reset();
    deliveryRepository = mock(NotificationDeliveryRepository.class);
    channelRepository = mock(NotificationChannelRepository.class);
    templateRepository = mock(NotificationTemplateRepository.class);
    when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service =
        new NotificationService(
            deliveryRepository,
            channelRepository,
            templateRepository,
            new NotificationChannelRegistry(List.of(stub)),
            new TemplateRenderer(),
            new ObjectMapper());
  }

  @AfterEach
  void tearDown() {
    ParkContext.clear();
  }

  @Test
  void sendsThroughConfiguredChannelWithRenderedTemplate() {
    when(channelRepository.findByIdAndParkId("ch-1", "PARK-A"))
        .thenReturn(Optional.of(channel("ch-1")));
    NotificationTemplateEntity template = new NotificationTemplateEntity();
    template.setId("tpl-1");
    template.setParkId("PARK-A");
    template.setBody("Alert ${eventType} @ ${location}");
    when(templateRepository.findByIdAndParkId("tpl-1", "PARK-A")).thenReturn(Optional.of(template));

    DeliveryView view = service.send(event(), "sub-1", "ch-1", "tpl-1");

    assertThat(view.status()).isEqualTo("SENT");
    assertThat(stub.last.content()).isEqualTo("Alert access.denied @ Gate 1");
  }

  @Test
  void recordsFailureWhenChannelNotFound() {
    when(channelRepository.findByIdAndParkId("missing", "PARK-A")).thenReturn(Optional.empty());

    DeliveryView view = service.send(event(), "sub-1", "missing", null);

    assertThat(view.status()).isEqualTo("FAILED");
    assertThat(view.error()).contains("not found");
  }

  @Test
  void resendReattemptsFailedDelivery() {
    NotificationDeliveryEntity delivery = new NotificationDeliveryEntity();
    delivery.setParkId("PARK-A");
    delivery.setEventId("evt-1");
    delivery.setChannelId("ch-1");
    delivery.setStatus("FAILED");
    delivery.setError("previous failure");
    delivery.setAttempts(1);
    delivery.setRenderedContent("previous content");
    delivery.setCreatedAt(Instant.now());
    delivery.setUpdatedAt(Instant.now());
    when(deliveryRepository.findByIdAndParkId(5L, "PARK-A")).thenReturn(Optional.of(delivery));
    when(channelRepository.findByIdAndParkId("ch-1", "PARK-A"))
        .thenReturn(Optional.of(channel("ch-1")));

    DeliveryView view = service.resend(5L);

    assertThat(view.status()).isEqualTo("SENT");
    assertThat(view.attempts()).isEqualTo(2);
  }

  private NotificationChannelEntity channel(String id) {
    NotificationChannelEntity channel = new NotificationChannelEntity();
    channel.setId(id);
    channel.setName("stub");
    channel.setType("STUB");
    channel.setParkId("PARK-A");
    channel.setConfig("{}");
    channel.setEnabled(true);
    return channel;
  }

  private CanonicalEvent event() {
    return new CanonicalEvent(
        "evt-1",
        "access.denied",
        "gate-01",
        SourceType.ACCESS,
        Instant.parse("2026-01-02T03:04:05Z"),
        EventSeverity.MAJOR,
        "PARK-A",
        "Gate 1",
        Map.of(),
        null,
        null);
  }

  private static final class StubChannel implements NotificationChannel {
    private NotificationMessage last;

    void reset() {
      last = null;
    }

    @Override
    public String type() {
      return "STUB";
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
      this.last = message;
      return DeliveryResult.ok();
    }
  }
}
