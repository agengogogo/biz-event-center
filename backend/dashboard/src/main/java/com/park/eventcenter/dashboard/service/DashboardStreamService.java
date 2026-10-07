package com.park.eventcenter.dashboard.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Pushes incremental dashboard situation updates to connected clients over SSE. */
@Service
public class DashboardStreamService {

  private final DashboardService dashboardService;
  private final Map<SseEmitter, String> emitters = new ConcurrentHashMap<>();

  public DashboardStreamService(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  public SseEmitter subscribe(String parkId) {
    SseEmitter emitter = new SseEmitter(0L);
    emitters.put(emitter, parkId);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(error -> emitters.remove(emitter));
    send(emitter, parkId);
    return emitter;
  }

  @Scheduled(fixedRate = 5000)
  public void push() {
    emitters.forEach(this::send);
  }

  private void send(SseEmitter emitter, String parkId) {
    try {
      emitter.send(
          SseEmitter.event().name("situation").data(dashboardService.situation(parkId, 20)));
    } catch (IOException | IllegalStateException ex) {
      emitters.remove(emitter);
      emitter.completeWithError(ex);
    }
  }
}
