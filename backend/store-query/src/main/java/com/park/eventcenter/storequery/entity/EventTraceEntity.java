package com.park.eventcenter.storequery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One step in an event's processing timeline (ingestion → dispatch → notification). */
@Entity
@Table(name = "event_trace")
public class EventTraceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, length = 64)
  private String eventId;

  @Column(name = "park_id", nullable = false, length = 64)
  private String parkId;

  @Column(name = "stage", nullable = false, length = 32)
  private String stage;

  @Column(name = "status", length = 32)
  private String status;

  @Column(name = "detail", length = 1024)
  private String detail;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected EventTraceEntity() {}

  public EventTraceEntity(String eventId, String parkId, String stage) {
    this.eventId = eventId;
    this.parkId = parkId;
    this.stage = stage;
  }

  public Long getId() {
    return id;
  }

  public String getEventId() {
    return eventId;
  }

  public String getParkId() {
    return parkId;
  }

  public String getStage() {
    return stage;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
