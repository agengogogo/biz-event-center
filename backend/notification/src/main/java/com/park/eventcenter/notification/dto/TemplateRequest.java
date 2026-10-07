package com.park.eventcenter.notification.dto;

import jakarta.validation.constraints.NotBlank;

/** Create/update payload for a notification template. */
public record TemplateRequest(
    @NotBlank String name, String channelType, String subject, @NotBlank String body) {}
