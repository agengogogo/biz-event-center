package com.park.eventcenter.dashboard.dto;

import com.park.eventcenter.storequery.dto.EventView;
import java.util.List;

/** Current active-alert situation for a park. */
public record SituationView(long activeCount, List<EventView> activeAlerts) {}
