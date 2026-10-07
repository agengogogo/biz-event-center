package com.park.eventcenter.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.error.NotFoundException;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.model.CanonicalEvent;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends notifications through configured channels and tracks delivery outcomes. */
@Service
public class NotificationService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final NotificationDeliveryRepository deliveryRepository;
  private final NotificationChannelRepository channelRepository;
  private final NotificationTemplateRepository templateRepository;
  private final NotificationChannelRegistry registry;
  private final TemplateRenderer renderer;
  private final ObjectMapper mapper;

  public NotificationService(
      NotificationDeliveryRepository deliveryRepository,
      NotificationChannelRepository channelRepository,
      NotificationTemplateRepository templateRepository,
      NotificationChannelRegistry registry,
      TemplateRenderer renderer,
      ObjectMapper mapper) {
    this.deliveryRepository = deliveryRepository;
    this.channelRepository = channelRepository;
    this.templateRepository = templateRepository;
    this.registry = registry;
    this.renderer = renderer;
    this.mapper = mapper;
  }

  @Transactional
  public DeliveryView send(
      CanonicalEvent event, String subscriptionId, String channelId, String templateId) {
    NotificationDeliveryEntity delivery = new NotificationDeliveryEntity();
    delivery.setParkId(event.parkId());
    delivery.setEventId(event.eventId());
    delivery.setSubscriptionId(subscriptionId);
    delivery.setChannelId(channelId);
    delivery.setAttempts(1);
    delivery.setCreatedAt(Instant.now());
    delivery.setUpdatedAt(Instant.now());

    Optional<NotificationChannelEntity> channel =
        channelId == null
            ? Optional.empty()
            : channelRepository.findByIdAndParkId(channelId, event.parkId());
    if (channel.isEmpty()) {
      delivery.setStatus("FAILED");
      delivery.setError("Notification channel not found: " + channelId);
      return toView(deliveryRepository.save(delivery));
    }

    NotificationTemplateEntity template =
        templateId == null
            ? null
            : templateRepository.findByIdAndParkId(templateId, event.parkId()).orElse(null);
    String subject =
        template == null ? defaultSubject(event) : renderer.render(template.getSubject(), event);
    String content =
        template == null ? defaultContent(event) : renderer.render(template.getBody(), event);
    delivery.setRenderedContent(content);

    Optional<NotificationChannel> transport = registry.find(channel.get().getType());
    if (transport.isEmpty()) {
      delivery.setStatus("FAILED");
      delivery.setError("Unsupported channel type: " + channel.get().getType());
      return toView(deliveryRepository.save(delivery));
    }

    NotificationMessage message =
        new NotificationMessage(subject, content, readConfig(channel.get().getConfig()), event);
    DeliveryResult result = transport.get().send(message);
    delivery.setStatus(result.success() ? "SENT" : "FAILED");
    delivery.setError(result.error());
    delivery.setUpdatedAt(Instant.now());
    return toView(deliveryRepository.save(delivery));
  }

  @Transactional
  public DeliveryView resend(Long deliveryId) {
    String parkId = ParkContext.require();
    NotificationDeliveryEntity delivery =
        deliveryRepository
            .findByIdAndParkId(deliveryId, parkId)
            .orElseThrow(
                () -> new NotFoundException("Notification delivery not found: " + deliveryId));
    NotificationChannelEntity channel =
        delivery.getChannelId() == null
            ? null
            : channelRepository.findByIdAndParkId(delivery.getChannelId(), parkId).orElse(null);
    delivery.setAttempts(delivery.getAttempts() + 1);
    delivery.setUpdatedAt(Instant.now());
    if (channel == null) {
      delivery.setStatus("FAILED");
      delivery.setError("Notification channel not found");
      return toView(deliveryRepository.save(delivery));
    }
    Optional<NotificationChannel> transport = registry.find(channel.getType());
    if (transport.isEmpty()) {
      delivery.setStatus("FAILED");
      delivery.setError("Unsupported channel type: " + channel.getType());
      return toView(deliveryRepository.save(delivery));
    }
    NotificationMessage message =
        new NotificationMessage(
            null, delivery.getRenderedContent(), readConfig(channel.getConfig()), null);
    DeliveryResult result = transport.get().send(message);
    delivery.setStatus(result.success() ? "SENT" : "FAILED");
    delivery.setError(result.error());
    return toView(deliveryRepository.save(delivery));
  }

  @Transactional(readOnly = true)
  public List<DeliveryView> list() {
    return deliveryRepository.findByParkIdOrderByCreatedAtDesc(ParkContext.require()).stream()
        .map(this::toView)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DeliveryView> listByEvent(String eventId) {
    return deliveryRepository
        .findByEventIdAndParkIdOrderByCreatedAtDesc(eventId, ParkContext.require())
        .stream()
        .map(this::toView)
        .toList();
  }

  private String defaultSubject(CanonicalEvent event) {
    return "["
        + (event.severity() == null ? "INFO" : event.severity().name())
        + "] "
        + event.eventType();
  }

  private String defaultContent(CanonicalEvent event) {
    return "Event "
        + event.eventId()
        + " ("
        + event.eventType()
        + ") occurred at "
        + event.location()
        + " with severity "
        + event.severity();
  }

  private Map<String, Object> readConfig(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return mapper.readValue(json, MAP_TYPE);
    } catch (JsonProcessingException ex) {
      return Map.of();
    }
  }

  private DeliveryView toView(NotificationDeliveryEntity entity) {
    return new DeliveryView(
        entity.getId(),
        entity.getParkId(),
        entity.getEventId(),
        entity.getSubscriptionId(),
        entity.getChannelId(),
        entity.getStatus(),
        entity.getError(),
        entity.getRenderedContent(),
        entity.getAttempts(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
