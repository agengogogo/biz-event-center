package com.park.eventcenter.storequery.entity;

import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.EventStatus;
import com.park.eventcenter.model.PayloadValidationStatus;
import com.park.eventcenter.model.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persisted canonical event. The event id is the primary key, giving natural idempotency. */
@Entity
@Table(name = "events")
public class EventEntity {

  @Id
  @Column(name = "event_id", length = 64)
  private String eventId;

  @Column(name = "event_type", nullable = false, length = 128)
  private String eventType;

  @Column(name = "source", length = 128)
  private String source;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 32)
  private SourceType sourceType;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "severity", nullable = false, length = 16)
  private EventSeverity severity;

  @Column(name = "park_id", nullable = false, length = 64)
  private String parkId;

  @Column(name = "location", length = 256)
  private String location;

  @Column(name = "payload", columnDefinition = "text")
  private String payload;

  @Column(name = "raw_payload", columnDefinition = "text")
  private String rawPayload;

  @Column(name = "trace_id", length = 128)
  private String traceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private EventStatus status;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type_validation", length = 16)
  private PayloadValidationStatus eventTypeValidation;

  @Column(name = "validation_reason", length = 512)
  private String validationReason;

  protected EventEntity() {}

  public EventEntity(String eventId, String parkId) {
    this.eventId = eventId;
    this.parkId = parkId;
  }

  public String getEventId() {
    return eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public SourceType getSourceType() {
    return sourceType;
  }

  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public EventSeverity getSeverity() {
    return severity;
  }

  public void setSeverity(EventSeverity severity) {
    this.severity = severity;
  }

  public String getParkId() {
    return parkId;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getRawPayload() {
    return rawPayload;
  }

  public void setRawPayload(String rawPayload) {
    this.rawPayload = rawPayload;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public EventStatus getStatus() {
    return status;
  }

  public void setStatus(EventStatus status) {
    this.status = status;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(Instant receivedAt) {
    this.receivedAt = receivedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public PayloadValidationStatus getEventTypeValidation() {
    return eventTypeValidation;
  }

  public void setEventTypeValidation(PayloadValidationStatus eventTypeValidation) {
    this.eventTypeValidation = eventTypeValidation;
  }

  public String getValidationReason() {
    return validationReason;
  }

  public void setValidationReason(String validationReason) {
    this.validationReason = validationReason;
  }
}
