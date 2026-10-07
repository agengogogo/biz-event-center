package com.park.eventcenter.storequery.dto;

/** Generic key/count projection used by dashboard aggregations. */
public record CountByKey(String key, long total) {}
