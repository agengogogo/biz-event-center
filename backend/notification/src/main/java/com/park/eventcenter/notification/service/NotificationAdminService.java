package com.park.eventcenter.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.common.error.NotFoundException;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.notification.dto.ChannelRequest;
import com.park.eventcenter.notification.dto.ChannelView;
import com.park.eventcenter.notification.dto.TemplateRequest;
import com.park.eventcenter.notification.dto.TemplateView;
import com.park.eventcenter.notification.entity.NotificationChannelEntity;
import com.park.eventcenter.notification.entity.NotificationTemplateEntity;
import com.park.eventcenter.notification.repository.NotificationChannelRepository;
import com.park.eventcenter.notification.repository.NotificationTemplateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD for notification channels and templates. */
@Service
public class NotificationAdminService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final NotificationChannelRepository channelRepository;
  private final NotificationTemplateRepository templateRepository;
  private final ObjectMapper mapper;

  public NotificationAdminService(
      NotificationChannelRepository channelRepository,
      NotificationTemplateRepository templateRepository,
      ObjectMapper mapper) {
    this.channelRepository = channelRepository;
    this.templateRepository = templateRepository;
    this.mapper = mapper;
  }

  @Transactional
  public ChannelView createChannel(ChannelRequest request) {
    NotificationChannelEntity entity = new NotificationChannelEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setParkId(ParkContext.require());
    entity.setCreatedAt(Instant.now());
    applyChannel(entity, request);
    return toChannelView(channelRepository.save(entity));
  }

  @Transactional
  public ChannelView updateChannel(String id, ChannelRequest request) {
    NotificationChannelEntity entity = requireChannel(id);
    applyChannel(entity, request);
    return toChannelView(channelRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public List<ChannelView> listChannels() {
    return channelRepository.findByParkIdOrderByNameAsc(ParkContext.require()).stream()
        .map(this::toChannelView)
        .toList();
  }

  @Transactional(readOnly = true)
  public ChannelView getChannel(String id) {
    return toChannelView(requireChannel(id));
  }

  @Transactional
  public ChannelView setChannelEnabled(String id, boolean enabled) {
    NotificationChannelEntity entity = requireChannel(id);
    entity.setEnabled(enabled);
    entity.setUpdatedAt(Instant.now());
    return toChannelView(channelRepository.save(entity));
  }

  @Transactional
  public void deleteChannel(String id) {
    channelRepository.delete(requireChannel(id));
  }

  @Transactional
  public TemplateView createTemplate(TemplateRequest request) {
    NotificationTemplateEntity entity = new NotificationTemplateEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setParkId(ParkContext.require());
    entity.setCreatedAt(Instant.now());
    applyTemplate(entity, request);
    return toTemplateView(templateRepository.save(entity));
  }

  @Transactional
  public TemplateView updateTemplate(String id, TemplateRequest request) {
    NotificationTemplateEntity entity = requireTemplate(id);
    applyTemplate(entity, request);
    return toTemplateView(templateRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public List<TemplateView> listTemplates() {
    return templateRepository.findByParkIdOrderByNameAsc(ParkContext.require()).stream()
        .map(this::toTemplateView)
        .toList();
  }

  @Transactional(readOnly = true)
  public TemplateView getTemplate(String id) {
    return toTemplateView(requireTemplate(id));
  }

  @Transactional
  public void deleteTemplate(String id) {
    templateRepository.delete(requireTemplate(id));
  }

  private void applyChannel(NotificationChannelEntity entity, ChannelRequest request) {
    entity.setName(request.name());
    entity.setType(request.type());
    entity.setConfig(writeConfig(request.config()));
    entity.setEnabled(request.enabled() == null || request.enabled());
    entity.setUpdatedAt(Instant.now());
  }

  private void applyTemplate(NotificationTemplateEntity entity, TemplateRequest request) {
    entity.setName(request.name());
    entity.setChannelType(request.channelType());
    entity.setSubject(request.subject());
    entity.setBody(request.body());
    entity.setUpdatedAt(Instant.now());
  }

  private NotificationChannelEntity requireChannel(String id) {
    return channelRepository
        .findByIdAndParkId(id, ParkContext.require())
        .orElseThrow(() -> new NotFoundException("Notification channel not found: " + id));
  }

  private NotificationTemplateEntity requireTemplate(String id) {
    return templateRepository
        .findByIdAndParkId(id, ParkContext.require())
        .orElseThrow(() -> new NotFoundException("Notification template not found: " + id));
  }

  private String writeConfig(Map<String, Object> config) {
    try {
      return mapper.writeValueAsString(config == null ? Map.of() : config);
    } catch (JsonProcessingException ex) {
      throw new BadRequestException("Invalid channel config: " + ex.getOriginalMessage());
    }
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

  private ChannelView toChannelView(NotificationChannelEntity entity) {
    return new ChannelView(
        entity.getId(),
        entity.getName(),
        entity.getType(),
        entity.getParkId(),
        readConfig(entity.getConfig()),
        entity.isEnabled(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private TemplateView toTemplateView(NotificationTemplateEntity entity) {
    return new TemplateView(
        entity.getId(),
        entity.getName(),
        entity.getChannelType(),
        entity.getSubject(),
        entity.getBody(),
        entity.getParkId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
