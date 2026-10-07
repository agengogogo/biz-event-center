package com.park.eventcenter.storequery.service;

import com.park.eventcenter.model.TraceStage;
import com.park.eventcenter.storequery.dto.TraceView;
import com.park.eventcenter.storequery.entity.EventTraceEntity;
import com.park.eventcenter.storequery.repository.EventTraceRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records and reads the processing timeline of an event. */
@Service
public class EventTraceService {

  private final EventTraceRepository repository;

  public EventTraceService(EventTraceRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void append(
      String eventId, String parkId, TraceStage stage, String status, String detail) {
    EventTraceEntity trace = new EventTraceEntity(eventId, parkId, stage.name());
    trace.setStatus(status);
    trace.setDetail(truncate(detail));
    trace.setOccurredAt(Instant.now());
    repository.save(trace);
  }

  @Transactional(readOnly = true)
  public List<TraceView> list(String eventId, String parkId) {
    return repository.findByEventIdAndParkIdOrderByOccurredAtAsc(eventId, parkId).stream()
        .map(t -> new TraceView(t.getStage(), t.getStatus(), t.getDetail(), t.getOccurredAt()))
        .toList();
  }

  private String truncate(String detail) {
    if (detail == null || detail.length() <= 1024) {
      return detail;
    }
    return detail.substring(0, 1024);
  }
}
