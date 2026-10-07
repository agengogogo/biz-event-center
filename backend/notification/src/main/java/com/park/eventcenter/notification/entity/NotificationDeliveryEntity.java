package com.park.eventcenter.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Record of one notification send attempt with its outcome. */
@Entity
@Table(name = "notification_delivery")
public class NotificationDeliveryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "park_id", nullable = false, length = 64)
  private String parkId;

  @Column(name = "event_id", nullable = false, length = 64)
  private String eventId;

  @Column(name = "subscription_id", length = 64)
  private String subscriptionId;

  @Column(name = "channel_id", length = 64)
  private String channelId;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @Column(name = "error", length = 2048)
  private String error;

  @Column(name = "rendered_content", columnDefinition = "text")
  private String renderedContent;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public String getParkId() {
    return parkId;
  }

  public void setParkId(String parkId) {
    this.parkId = parkId;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getRenderedContent() {
    return renderedContent;
  }

  public void setRenderedContent(String renderedContent) {
    this.renderedContent = renderedContent;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
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
