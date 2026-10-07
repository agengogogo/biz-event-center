package com.park.eventcenter.storequery.repository;

import com.park.eventcenter.storequery.entity.AuditEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Audit trail access. */
public interface AuditRepository extends JpaRepository<AuditEntity, Long> {

  List<AuditEntity> findByParkIdOrderByOccurredAtDesc(String parkId, Pageable pageable);
}
