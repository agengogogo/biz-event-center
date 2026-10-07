package com.park.eventcenter.dispatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Delivery attempt record for one event routed to one subscription target. */
@Entity
@Table(name = "delivery")
public class DeliveryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "park_id", nullable = false, length = 64)
  private String parkId;

  @Column(name = "event_id", nullable = false, length = 64)
  private String eventId;

  @Column(name = "subscription_id", nullable = false, length = 64)
  private String subscriptionId;

  @Column(name = "target_type", nullable = false, length = 32)
  private String targetType;

  @Column(name = "target", length = 512)
  private String target;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "last_error", length = 2048)
  private String lastError;

  @Column(name = "dead_letter", nullable = false)
  private boolean deadLetter;

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

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public boolean isDeadLetter() {
    return deadLetter;
  }

  public void setDeadLetter(boolean deadLetter) {
    this.deadLetter = deadLetter;
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
