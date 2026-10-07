package com.park.eventcenter.dashboard.api;

import com.park.eventcenter.common.security.AuthenticatedUser;
import com.park.eventcenter.dashboard.dto.DashboardStatistics;
import com.park.eventcenter.dashboard.dto.SituationView;
import com.park.eventcenter.dashboard.service.DashboardService;
import com.park.eventcenter.dashboard.service.DashboardStreamService;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Client-agnostic dashboard data service and incremental push endpoint. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;
  private final DashboardStreamService streamService;

  public DashboardController(
      DashboardService dashboardService, DashboardStreamService streamService) {
    this.dashboardService = dashboardService;
    this.streamService = streamService;
  }

  @GetMapping("/statistics")
  public DashboardStatistics statistics(
      @RequestParam(defaultValue = "TYPE") String dimension,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "hour") String interval) {
    return dashboardService.statistics(dimension, from, to, interval);
  }

  @GetMapping("/situation")
  public SituationView situation(@RequestParam(defaultValue = "50") int limit) {
    return dashboardService.situation(limit);
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser user) {
    return streamService.subscribe(user == null ? "default" : user.parkId());
  }
}
