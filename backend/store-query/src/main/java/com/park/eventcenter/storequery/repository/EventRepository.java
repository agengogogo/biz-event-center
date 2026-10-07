package com.park.eventcenter.storequery.repository;

import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.EventStatus;
import com.park.eventcenter.storequery.dto.AggregationRow;
import com.park.eventcenter.storequery.entity.EventEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Event persistence and retrieval, scoped by park through query predicates. */
public interface EventRepository
    extends JpaRepository<EventEntity, String>, JpaSpecificationExecutor<EventEntity> {

  Optional<EventEntity> findByEventIdAndParkId(String eventId, String parkId);

  boolean existsByEventIdAndParkId(String eventId, String parkId);

  long countByParkIdAndStatusIn(String parkId, Collection<EventStatus> statuses);

  List<EventEntity> findByParkIdAndSeverityInAndStatusNotOrderByOccurredAtDesc(
      String parkId, Collection<EventSeverity> severities, EventStatus status, Pageable pageable);

  @Query(
      "select new com.park.eventcenter.storequery.dto.AggregationRow(e.eventType, count(e)) "
          + "from EventEntity e where e.parkId = :parkId and e.occurredAt between :from and :to "
          + "group by e.eventType order by count(e) desc")
  List<AggregationRow> countByEventType(
      @Param("parkId") String parkId, @Param("from") Instant from, @Param("to") Instant to);

  @Query(
      "select new com.park.eventcenter.storequery.dto.AggregationRow(coalesce(e.source, 'UNKNOWN'), count(e)) "
          + "from EventEntity e where e.parkId = :parkId and e.occurredAt between :from and :to "
          + "group by e.source order by count(e) desc")
  List<AggregationRow> countBySource(
      @Param("parkId") String parkId, @Param("from") Instant from, @Param("to") Instant to);

  @Query(
      "select new com.park.eventcenter.storequery.dto.AggregationRow(e.severity, count(e)) "
          + "from EventEntity e where e.parkId = :parkId and e.occurredAt between :from and :to "
          + "group by e.severity order by count(e) desc")
  List<AggregationRow> countBySeverity(
      @Param("parkId") String parkId, @Param("from") Instant from, @Param("to") Instant to);

  @Query(
      "select new com.park.eventcenter.storequery.dto.AggregationRow(coalesce(e.location, 'UNKNOWN'), count(e)) "
          + "from EventEntity e where e.parkId = :parkId and e.occurredAt between :from and :to "
          + "group by e.location order by count(e) desc")
  List<AggregationRow> countByLocation(
      @Param("parkId") String parkId, @Param("from") Instant from, @Param("to") Instant to);

  @Query(
      "select e.occurredAt from EventEntity e "
          + "where e.parkId = :parkId and e.occurredAt between :from and :to order by e.occurredAt asc")
  List<Instant> findOccurredAt(
      @Param("parkId") String parkId, @Param("from") Instant from, @Param("to") Instant to);
}
