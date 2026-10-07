package com.park.eventcenter.storequery.dto;

/** Raw aggregation row; the key may be a string or an enum depending on the dimension. */
public record AggregationRow(Object key, long total) {}
