package com.park.eventcenter.storequery.repository;

import com.park.eventcenter.storequery.entity.EventTraceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Append-only access to the event processing timeline. */
public interface EventTraceRepository extends JpaRepository<EventTraceEntity, Long> {

  List<EventTraceEntity> findByEventIdAndParkIdOrderByOccurredAtAsc(String eventId, String parkId);
}
