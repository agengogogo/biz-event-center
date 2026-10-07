package com.park.eventcenter.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Reusable notification template; the body may reference event fields via {@code ${...}}. */
@Entity
@Table(name = "notification_template")
public class NotificationTemplateEntity {

  @Id
  @Column(name = "id", length = 64)
  private String id;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "channel_type", length = 32)
  private String channelType;

  @Column(name = "subject", length = 512)
  private String subject;

  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "park_id", nullable = false, length = 64)
  private String parkId;

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

  public String getChannelType() {
    return channelType;
  }

  public void setChannelType(String channelType) {
    this.channelType = channelType;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getParkId() {
    return parkId;
  }

  public void setParkId(String parkId) {
    this.parkId = parkId;
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
