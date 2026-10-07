package com.park.eventcenter.notification.channel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Resolves the configured transport for a channel type. */
@Component
public class NotificationChannelRegistry {

  private final Map<String, NotificationChannel> channels;

  public NotificationChannelRegistry(List<NotificationChannel> channels) {
    this.channels =
        channels.stream()
            .collect(
                Collectors.toMap(
                    channel -> channel.type().toUpperCase(java.util.Locale.ROOT),
                    Function.identity()));
  }

  public Optional<NotificationChannel> find(String type) {
    if (type == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(channels.get(type.toUpperCase(java.util.Locale.ROOT)));
  }

  public boolean supports(String type) {
    return find(type).isPresent();
  }
}
