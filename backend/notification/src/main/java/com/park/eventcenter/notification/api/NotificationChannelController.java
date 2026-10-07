package com.park.eventcenter.notification.api;

import com.park.eventcenter.notification.dto.ChannelRequest;
import com.park.eventcenter.notification.dto.ChannelView;
import com.park.eventcenter.notification.service.NotificationAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Notification channel management endpoints. */
@RestController
@RequestMapping("/api/notification-channels")
public class NotificationChannelController {

  private final NotificationAdminService service;

  public NotificationChannelController(NotificationAdminService service) {
    this.service = service;
  }

  @GetMapping
  public List<ChannelView> list() {
    return service.listChannels();
  }

  @GetMapping("/{id}")
  public ChannelView get(@PathVariable String id) {
    return service.getChannel(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ChannelView create(@RequestBody @Valid ChannelRequest request) {
    return service.createChannel(request);
  }

  @PutMapping("/{id}")
  public ChannelView update(@PathVariable String id, @RequestBody @Valid ChannelRequest request) {
    return service.updateChannel(id, request);
  }

  @PostMapping("/{id}/enable")
  public ChannelView enable(@PathVariable String id) {
    return service.setChannelEnabled(id, true);
  }

  @PostMapping("/{id}/disable")
  public ChannelView disable(@PathVariable String id) {
    return service.setChannelEnabled(id, false);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.deleteChannel(id);
  }
}
