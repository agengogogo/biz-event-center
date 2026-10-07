package com.park.eventcenter.ingestion.dto;

import com.park.eventcenter.model.SourceType;

/** Admin request to register/update an event source. */
public record SourceRequest(String name, SourceType sourceType, Boolean enabled) {}
