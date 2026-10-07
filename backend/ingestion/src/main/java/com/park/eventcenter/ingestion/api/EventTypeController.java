package com.park.eventcenter.ingestion.api;

import com.park.eventcenter.ingestion.dto.EventTypeRequest;
import com.park.eventcenter.ingestion.dto.EventTypeView;
import com.park.eventcenter.ingestion.service.EventTypeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Business event type catalog and lifecycle endpoints. */
@RestController
@RequestMapping("/api/event-types")
public class EventTypeController {

  private final EventTypeService service;

  public EventTypeController(EventTypeService service) {
    this.service = service;
  }

  @GetMapping
  public List<EventTypeView> list(@RequestParam(required = false) Boolean enabled) {
    return service.list(enabled);
  }

  @GetMapping("/{type}")
  public EventTypeView get(@PathVariable String type) {
    return service.get(type);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EventTypeView create(@RequestBody EventTypeRequest request) {
    return service.create(request);
  }

  @PutMapping("/{type}")
  public EventTypeView update(@PathVariable String type, @RequestBody EventTypeRequest request) {
    return service.update(type, request);
  }

  @PostMapping("/{type}/enable")
  public EventTypeView enable(@PathVariable String type) {
    return service.setEnabled(type, true);
  }

  @PostMapping("/{type}/disable")
  public EventTypeView disable(@PathVariable String type) {
    return service.setEnabled(type, false);
  }
}
