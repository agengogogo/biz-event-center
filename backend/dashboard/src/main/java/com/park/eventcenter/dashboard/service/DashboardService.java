package com.park.eventcenter.dashboard.service;

import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.dashboard.dto.DashboardStatistics;
import com.park.eventcenter.dashboard.dto.SituationView;
import com.park.eventcenter.dashboard.dto.StatisticPoint;
import com.park.eventcenter.model.EventSeverity;
import com.park.eventcenter.model.EventStatus;
import com.park.eventcenter.storequery.dto.AggregationRow;
import com.park.eventcenter.storequery.entity.EventEntity;
import com.park.eventcenter.storequery.repository.EventRepository;
import com.park.eventcenter.storequery.service.EventMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Computes statistics aggregations and active-alert situation for the dashboard. */
@Service
public class DashboardService {

  private static final List<EventSeverity> HIGH_SEVERITIES =
      List.of(EventSeverity.MAJOR, EventSeverity.CRITICAL);
  private static final List<EventStatus> ACTIVE_STATUSES =
      List.of(
          EventStatus.RECEIVED, EventStatus.DISPATCHED, EventStatus.DELIVERED, EventStatus.FAILED);

  private final EventRepository repository;
  private final EventMapper mapper;

  public DashboardService(EventRepository repository, EventMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public DashboardStatistics statistics(
      String dimension, Instant from, Instant to, String interval) {
    return statistics(ParkContext.require(), dimension, from, to, interval);
  }

  @Transactional(readOnly = true)
  public DashboardStatistics statistics(
      String parkId, String dimension, Instant from, Instant to, String interval) {
    Instant toValue = to == null ? Instant.now() : to;
    Instant fromValue = from == null ? toValue.minus(Duration.ofHours(24)) : from;
    String dim = dimension == null ? "TYPE" : dimension.toUpperCase();
    List<StatisticPoint> points =
        switch (dim) {
          case "TYPE" -> mapRows(repository.countByEventType(parkId, fromValue, toValue));
          case "SOURCE" -> mapRows(repository.countBySource(parkId, fromValue, toValue));
          case "SEVERITY" -> mapRows(repository.countBySeverity(parkId, fromValue, toValue));
          case "LOCATION" -> mapRows(repository.countByLocation(parkId, fromValue, toValue));
          case "TIME" ->
              timeSeries(repository.findOccurredAt(parkId, fromValue, toValue), interval);
          default -> throw new BadRequestException("Unsupported dimension: " + dimension);
        };
    long total = points.stream().mapToLong(StatisticPoint::total).sum();
    return new DashboardStatistics(fromValue, toValue, dim, total, points);
  }

  @Transactional(readOnly = true)
  public SituationView situation(int limit) {
    return situation(ParkContext.require(), limit);
  }

  @Transactional(readOnly = true)
  public SituationView situation(String parkId, int limit) {
    List<EventEntity> alerts =
        repository.findByParkIdAndSeverityInAndStatusNotOrderByOccurredAtDesc(
            parkId, HIGH_SEVERITIES, EventStatus.CLOSED, PageRequest.of(0, limit));
    long activeCount = repository.countByParkIdAndStatusIn(parkId, ACTIVE_STATUSES);
    return new SituationView(activeCount, alerts.stream().map(mapper::toView).toList());
  }

  private List<StatisticPoint> mapRows(List<AggregationRow> rows) {
    return rows.stream()
        .map(row -> new StatisticPoint(String.valueOf(row.key()), row.total()))
        .toList();
  }

  private List<StatisticPoint> timeSeries(List<Instant> instants, String interval) {
    boolean daily = "day".equalsIgnoreCase(interval);
    ChronoUnit unit = daily ? ChronoUnit.DAYS : ChronoUnit.HOURS;
    Map<String, Long> buckets = new LinkedHashMap<>();
    for (Instant instant : instants) {
      String key = instant.atZone(ZoneOffset.UTC).truncatedTo(unit).toInstant().toString();
      buckets.merge(key, 1L, Long::sum);
    }
    return buckets.entrySet().stream()
        .map(entry -> new StatisticPoint(entry.getKey(), entry.getValue()))
        .toList();
  }
}
