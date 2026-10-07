package com.park.eventcenter.subscription.dto;

/** One declarative field-mapping rule applied to an event payload before delivery. */
public record TransformRule(String from, String to, Object defaultValue) {}
