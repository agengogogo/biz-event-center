package com.park.eventcenter.notification.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** Create/update payload for a notification channel. */
public record ChannelRequest(
    @NotBlank String name, @NotBlank String type, Map<String, Object> config, Boolean enabled) {}
