package com.park.eventcenter.storequery.service;

import com.park.eventcenter.common.api.PageResponse;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.storequery.dto.EventSearchCriteria;
import com.park.eventcenter.storequery.dto.EventView;
import com.park.eventcenter.storequery.entity.EventEntity;
import com.park.eventcenter.storequery.repository.EventRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Conditional event search scoped to the caller's park. */
@Service
public class EventQueryService {

  private static final int MAX_SIZE = 200;

  private final EventRepository repository;
  private final EventMapper mapper;
  private final EventStoreService storeService;

  public EventQueryService(
      EventRepository repository, EventMapper mapper, EventStoreService storeService) {
    this.repository = repository;
    this.mapper = mapper;
    this.storeService = storeService;
  }

  @Transactional(readOnly = true)
  public PageResponse<EventView> search(EventSearchCriteria criteria) {
    return search(ParkContext.require(), criteria);
  }

  @Transactional(readOnly = true)
  public PageResponse<EventView> search(String parkId, EventSearchCriteria criteria) {
    int size = Math.min(Math.max(criteria.size(), 1), MAX_SIZE);
    PageRequest pageRequest =
        PageRequest.of(
            Math.max(criteria.page(), 0), size, Sort.by(Sort.Direction.DESC, "occurredAt"));
    Page<EventEntity> page = repository.findAll(specification(parkId, criteria), pageRequest);
    List<EventView> items = page.getContent().stream().map(mapper::toView).toList();
    return PageResponse.of(items, page.getTotalElements(), page.getNumber(), page.getSize());
  }

  @Transactional(readOnly = true)
  public EventView get(String eventId) {
    return storeService.getById(eventId, ParkContext.require());
  }

  private Specification<EventEntity> specification(String parkId, EventSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("parkId"), parkId));
      if (criteria.from() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), criteria.from()));
      }
      if (criteria.to() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), criteria.to()));
      }
      if (criteria.eventType() != null && !criteria.eventType().isBlank()) {
        predicates.add(cb.equal(root.get("eventType"), criteria.eventType()));
      }
      if (criteria.source() != null && !criteria.source().isBlank()) {
        predicates.add(cb.equal(root.get("source"), criteria.source()));
      }
      if (criteria.severity() != null) {
        predicates.add(cb.equal(root.get("severity"), criteria.severity()));
      }
      if (criteria.location() != null && !criteria.location().isBlank()) {
        predicates.add(
            cb.like(cb.lower(root.get("location")), "%" + criteria.location().toLowerCase() + "%"));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
