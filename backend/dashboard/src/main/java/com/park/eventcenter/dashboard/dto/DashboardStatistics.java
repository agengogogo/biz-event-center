package com.park.eventcenter.dashboard.dto;

import java.time.Instant;
import java.util.List;

/** Aggregated event statistics for a time range and dimension. */
public record DashboardStatistics(
    Instant from, Instant to, String dimension, long total, List<StatisticPoint> points) {}
