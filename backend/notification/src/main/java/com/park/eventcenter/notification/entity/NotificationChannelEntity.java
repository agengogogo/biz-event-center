package com.park.eventcenter.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Configured outbound notification channel (webhook, WeChat Work, email, SMS...). */
@Entity
@Table(name = "notification_channel")
public class NotificationChannelEntity {

  @Id
  @Column(name = "id", length = 64)
  private String id;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "type", nullable = false, length = 32)
  private String type;

  @Column(name = "park_id", nullable = false, length = 64)
  private String parkId;

  @Column(name = "config", columnDefinition = "text")
  private String config;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getParkId() {
    return parkId;
  }

  public void setParkId(String parkId) {
    this.parkId = parkId;
  }

  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
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
