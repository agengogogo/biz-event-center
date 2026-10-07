package com.park.eventcenter.ingestion.repository;

import com.park.eventcenter.ingestion.entity.SourceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Registered source persistence. */
public interface SourceRepository extends JpaRepository<SourceEntity, String> {

  List<SourceEntity> findByParkIdOrderByCreatedAtDesc(String parkId);

  Optional<SourceEntity> findByIdAndParkId(String id, String parkId);
}
