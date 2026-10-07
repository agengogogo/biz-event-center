package com.park.eventcenter.storequery.api;

import com.park.eventcenter.common.api.PageResponse;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.storequery.dto.EventSearchCriteria;
import com.park.eventcenter.storequery.dto.EventView;
import com.park.eventcenter.storequery.dto.TraceView;
import com.park.eventcenter.storequery.service.EventQueryService;
import com.park.eventcenter.storequery.service.EventTraceService;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Conditional event retrieval and trace endpoints. */
@RestController
@RequestMapping("/api/events")
public class EventQueryController {

  private final EventQueryService queryService;
  private final EventTraceService traceService;

  public EventQueryController(EventQueryService queryService, EventTraceService traceService) {
    this.queryService = queryService;
    this.traceService = traceService;
  }

  @GetMapping
  public PageResponse<EventView> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false, name = "type") String eventType,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) EventSeverity severity,
      @RequestParam(required = false) String location,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    EventSearchCriteria criteria =
        new EventSearchCriteria(from, to, eventType, source, severity, location, page, size);
    return queryService.search(criteria);
  }

  @GetMapping("/{eventId}")
  public EventView get(@PathVariable String eventId) {
    return queryService.get(eventId);
  }

  @GetMapping("/{eventId}/traces")
  public List<TraceView> traces(@PathVariable String eventId) {
    String parkId = ParkContext.require();
    queryService.get(eventId);
    return traceService.list(eventId, parkId);
  }
}
