package com.park.eventcenter.ingestion.entity;

import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Definition of a business event type and the JSON Schema its payload must satisfy. */
@Entity
@Table(name = "event_type")
public class EventTypeEntity {

  @Id
  @Column(name = "type", length = 128)
  private String type;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_category", nullable = false, length = 32)
  private SourceType sourceCategory;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_severity", nullable = false, length = 16)
  private EventSeverity defaultSeverity;

  @Column(name = "payload_schema", columnDefinition = "text")
  private String payloadSchema;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "version", nullable = false)
  private int version = 1;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SourceType getSourceCategory() {
    return sourceCategory;
  }

  public void setSourceCategory(SourceType sourceCategory) {
    this.sourceCategory = sourceCategory;
  }

  public EventSeverity getDefaultSeverity() {
    return defaultSeverity;
  }

  public void setDefaultSeverity(EventSeverity defaultSeverity) {
    this.defaultSeverity = defaultSeverity;
  }

  public String getPayloadSchema() {
    return payloadSchema;
  }

  public void setPayloadSchema(String payloadSchema) {
    this.payloadSchema = payloadSchema;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
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
