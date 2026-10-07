package com.park.eventcenter.ingestion.repository;

import com.park.eventcenter.ingestion.entity.EventTypeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for business event type definitions. */
public interface EventTypeRepository extends JpaRepository<EventTypeEntity, String> {

  List<EventTypeEntity> findAllByOrderByTypeAsc();

  List<EventTypeEntity> findByEnabledTrueOrderByTypeAsc();
}
