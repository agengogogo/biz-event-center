package com.park.eventcenter.ingestion.api;

import com.park.eventcenter.ingestion.dto.IngestResponse;
import com.park.eventcenter.ingestion.service.IngestionService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified ingestion endpoint for external sources. Credentials are validated per source; the raw
 * document is accepted as a free-form map so heterogeneous payloads can be standardized.
 */
@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

  private final IngestionService ingestionService;

  public IngestionController(IngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @PostMapping("/events")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public IngestResponse ingest(
      @RequestHeader(name = "X-Source-Id", required = false) String sourceId,
      @RequestHeader(name = "X-Source-Secret", required = false) String sourceSecret,
      @RequestBody Map<String, Object> raw) {
    return ingestionService.ingest(sourceId, sourceSecret, raw);
  }
}
