package com.park.eventcenter.subscription.api;

import com.park.eventcenter.subscription.dto.SubscriptionRequest;
import com.park.eventcenter.subscription.dto.SubscriptionView;
import com.park.eventcenter.subscription.service.SubscriptionService;
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

/** Subscription CRUD and enable/disable endpoints. */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

  private final SubscriptionService service;

  public SubscriptionController(SubscriptionService service) {
    this.service = service;
  }

  @GetMapping
  public List<SubscriptionView> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public SubscriptionView get(@PathVariable String id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SubscriptionView create(@RequestBody @Valid SubscriptionRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public SubscriptionView update(
      @PathVariable String id, @RequestBody @Valid SubscriptionRequest request) {
    return service.update(id, request);
  }

  @PostMapping("/{id}/enable")
  public SubscriptionView enable(@PathVariable String id) {
    return service.setEnabled(id, true);
  }

  @PostMapping("/{id}/disable")
  public SubscriptionView disable(@PathVariable String id) {
    return service.setEnabled(id, false);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
