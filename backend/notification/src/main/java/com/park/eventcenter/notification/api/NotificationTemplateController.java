package com.park.eventcenter.notification.api;

import com.park.eventcenter.notification.dto.TemplateRequest;
import com.park.eventcenter.notification.dto.TemplateView;
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

/** Notification template management endpoints. */
@RestController
@RequestMapping("/api/notification-templates")
public class NotificationTemplateController {

  private final NotificationAdminService service;

  public NotificationTemplateController(NotificationAdminService service) {
    this.service = service;
  }

  @GetMapping
  public List<TemplateView> list() {
    return service.listTemplates();
  }

  @GetMapping("/{id}")
  public TemplateView get(@PathVariable String id) {
    return service.getTemplate(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TemplateView create(@RequestBody @Valid TemplateRequest request) {
    return service.createTemplate(request);
  }

  @PutMapping("/{id}")
  public TemplateView update(@PathVariable String id, @RequestBody @Valid TemplateRequest request) {
    return service.updateTemplate(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.deleteTemplate(id);
  }
}
