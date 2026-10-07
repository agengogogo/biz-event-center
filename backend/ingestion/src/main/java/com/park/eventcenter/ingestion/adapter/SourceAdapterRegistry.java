package com.park.eventcenter.ingestion.adapter;

import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.ingestion.entity.SourceEntity;
import com.park.eventcenter.model.CanonicalEvent;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Selects the adapter that supports a source's type. */
@Component
public class SourceAdapterRegistry {

  private final List<SourceAdapter> adapters;

  public SourceAdapterRegistry(List<SourceAdapter> adapters) {
    this.adapters = adapters;
  }

  public CanonicalEvent adapt(SourceEntity source, Map<String, Object> raw) {
    return adapters.stream()
        .filter(adapter -> adapter.supports(source.getSourceType()))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException("No adapter for source type: " + source.getSourceType()))
        .adapt(source, raw);
  }
}
