package com.park.eventcenter.storequery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Immutable audit record of a state-changing (or security-relevant) operation. */
@Entity
@Table(name = "audit_log")
public class AuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "actor", length = 128)
  private String actor;

  @Column(name = "action", nullable = false, length = 64)
  private String action;

  @Column(name = "resource_type", nullable = false, length = 64)
  private String resourceType;

  @Column(name = "resource_id", length = 128)
  private String resourceId;

  @Column(name = "park_id", length = 64)
  private String parkId;

  @Column(name = "detail", length = 2048)
  private String detail;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected AuditEntity() {}

  public AuditEntity(String action, String resourceType, String resourceId, String parkId) {
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.parkId = parkId;
  }

  public Long getId() {
    return id;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getParkId() {
    return parkId;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
