package com.park.eventcenter.notification.service;

import com.park.eventcenter.model.CanonicalEvent;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Renders {@code ${field}} placeholders in notification templates from event fields. */
@Component
public class TemplateRenderer {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

  public String render(String template, CanonicalEvent event) {
    if (template == null) {
      return null;
    }
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder builder = new StringBuilder();
    while (matcher.find()) {
      Object value = resolve(event, matcher.group(1).trim());
      matcher.appendReplacement(
          builder, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
    }
    matcher.appendTail(builder);
    return builder.toString();
  }

  private Object resolve(CanonicalEvent event, String path) {
    if (path.startsWith("payload.")) {
      return navigate(event.payload(), path.substring("payload.".length()));
    }
    return switch (path) {
      case "eventId" -> event.eventId();
      case "eventType" -> event.eventType();
      case "source" -> event.source();
      case "sourceType" -> event.sourceType() == null ? null : event.sourceType().name();
      case "occurredAt" -> event.occurredAt();
      case "severity" -> event.severity() == null ? null : event.severity().name();
      case "parkId" -> event.parkId();
      case "location" -> event.location();
      case "traceId" -> event.traceId();
      default -> null;
    };
  }

  private Object navigate(Map<String, Object> source, String dotted) {
    if (source == null) {
      return null;
    }
    Object current = source;
    for (String segment : dotted.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        return null;
      }
      current = map.get(segment);
    }
    return current;
  }
}
