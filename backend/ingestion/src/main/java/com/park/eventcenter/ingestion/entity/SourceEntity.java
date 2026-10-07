package com.park.eventcenter.ingestion.entity;

import com.park.eventcenter.model.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A registered event source with hashed credentials, bound to a park. */
@Entity
@Table(name = "event_source")
public class SourceEntity {

  @Id
  @Column(name = "id", length = 64)
  private String id;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 32)
  private SourceType sourceType;

  @Column(name = "park_id", nullable = false, length = 64)
  private String parkId;

  @Column(name = "secret_hash", nullable = false, length = 128)
  private String secretHash;

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

  public SourceType getSourceType() {
    return sourceType;
  }

  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  public String getParkId() {
    return parkId;
  }

  public void setParkId(String parkId) {
    this.parkId = parkId;
  }

  public String getSecretHash() {
    return secretHash;
  }

  public void setSecretHash(String secretHash) {
    this.secretHash = secretHash;
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
