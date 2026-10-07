package com.park.eventcenter.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A subscription routes matching events to a delivery target with optional filter/transform. */
@Entity
@Table(name = "subscription")
public class SubscriptionEntity {

  @Id
  @Column(name = "id", length = 64)
  private String id;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "park_id", nullable = false, length = 64)
  private String parkId;

  @Column(name = "event_type", nullable = false, length = 128)
  private String eventType;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "filter_expression", length = 2048)
  private String filterExpression;

  @Column(name = "transform_rules", columnDefinition = "text")
  private String transformRules;

  @Column(name = "target_type", nullable = false, length = 32)
  private String targetType;

  @Column(name = "target_url", length = 512)
  private String targetUrl;

  @Column(name = "notification_channel_id", length = 64)
  private String notificationChannelId;

  @Column(name = "template_id", length = 64)
  private String templateId;

  @Column(name = "ordered_delivery", nullable = false)
  private boolean orderedDelivery;

  @Column(name = "max_retries", nullable = false)
  private int maxRetries = 3;

  @Column(name = "backoff_millis", nullable = false)
  private long backoffMillis = 1000L;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getParkId() {
    return parkId;
  }

  public void setParkId(String parkId) {
    this.parkId = parkId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getFilterExpression() {
    return filterExpression;
  }

  public void setFilterExpression(String filterExpression) {
    this.filterExpression = filterExpression;
  }

  public String getTransformRules() {
    return transformRules;
  }

  public void setTransformRules(String transformRules) {
    this.transformRules = transformRules;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public String getTargetUrl() {
    return targetUrl;
  }

  public void setTargetUrl(String targetUrl) {
    this.targetUrl = targetUrl;
  }

  public String getNotificationChannelId() {
    return notificationChannelId;
  }

  public void setNotificationChannelId(String notificationChannelId) {
    this.notificationChannelId = notificationChannelId;
  }

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  public boolean isOrderedDelivery() {
    return orderedDelivery;
  }

  public void setOrderedDelivery(boolean orderedDelivery) {
    this.orderedDelivery = orderedDelivery;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public long getBackoffMillis() {
    return backoffMillis;
  }

  public void setBackoffMillis(long backoffMillis) {
    this.backoffMillis = backoffMillis;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
