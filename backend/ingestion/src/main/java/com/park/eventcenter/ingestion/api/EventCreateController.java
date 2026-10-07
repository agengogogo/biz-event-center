package com.park.eventcenter.ingestion.api;

import com.park.eventcenter.common.security.AuthenticatedUser;
import com.park.eventcenter.ingestion.dto.CreateEventRequest;
import com.park.eventcenter.ingestion.dto.IngestResponse;
import com.park.eventcenter.ingestion.service.IngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Management API for creating canonical events directly. The admin front-end is only one client;
 * any authenticated caller can use the same endpoint, and created events join the same pipeline.
 */
@RestController
@RequestMapping("/api/events")
public class EventCreateController {

  private final IngestionService ingestionService;

  public EventCreateController(IngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public IngestResponse create(
      @AuthenticationPrincipal AuthenticatedUser user, @RequestBody CreateEventRequest request) {
    String actor = user == null ? "api" : user.username();
    return ingestionService.create(request, actor);
  }
}
