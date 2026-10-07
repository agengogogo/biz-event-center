package com.park.eventcenter.ingestion.api;

import com.park.eventcenter.ingestion.dto.SourceRequest;
import com.park.eventcenter.ingestion.dto.SourceView;
import com.park.eventcenter.ingestion.service.SourceService;
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

/** Source registration and lifecycle endpoints. */
@RestController
@RequestMapping("/api/sources")
public class SourceAdminController {

  private final SourceService service;

  public SourceAdminController(SourceService service) {
    this.service = service;
  }

  @GetMapping
  public List<SourceView> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public SourceView get(@PathVariable String id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SourceView create(@RequestBody SourceRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public SourceView update(@PathVariable String id, @RequestBody SourceRequest request) {
    return service.update(id, request);
  }

  @PostMapping("/{id}/enable")
  public SourceView enable(@PathVariable String id) {
    return service.setEnabled(id, true);
  }

  @PostMapping("/{id}/disable")
  public SourceView disable(@PathVariable String id) {
    return service.setEnabled(id, false);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
