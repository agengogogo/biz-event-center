package com.park.eventcenter.storequery.service;

import com.park.eventcenter.storequery.entity.AuditEntity;
import com.park.eventcenter.storequery.repository.AuditRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes and reads the audit trail. */
@Service
public class AuditService {

  private final AuditRepository repository;

  public AuditService(AuditRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void record(
      String actor,
      String action,
      String resourceType,
      String resourceId,
      String parkId,
      String detail) {
    AuditEntity entity = new AuditEntity(action, resourceType, resourceId, parkId);
    entity.setActor(actor);
    entity.setDetail(detail);
    entity.setOccurredAt(Instant.now());
    repository.save(entity);
  }

  @Transactional(readOnly = true)
  public List<AuditEntity> recent(String parkId, int limit) {
    return repository.findByParkIdOrderByOccurredAtDesc(parkId, PageRequest.of(0, limit));
  }
}
