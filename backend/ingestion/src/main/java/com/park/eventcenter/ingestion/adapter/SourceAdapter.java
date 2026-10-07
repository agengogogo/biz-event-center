package com.park.eventcenter.ingestion.adapter;

import com.park.eventcenter.ingestion.entity.SourceEntity;
import com.park.eventcenter.model.CanonicalEvent;
import com.park.eventcenter.model.SourceType;
import java.util.Map;

/** Maps a source-specific raw document into the canonical event envelope. */
public interface SourceAdapter {

  boolean supports(SourceType sourceType);

  CanonicalEvent adapt(SourceEntity source, Map<String, Object> raw);
}
