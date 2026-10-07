package com.park.eventcenter.notification.channel;

import com.park.eventcenter.model.CanonicalEvent;
import java.util.Map;

/** A rendered notification ready to be sent through a specific channel. */
public record NotificationMessage(
    String subject, String content, Map<String, Object> config, CanonicalEvent event) {}
