package com.park.eventcenter.notification.channel;

import java.util.Map;

/** Helpers for reading loosely-typed channel configuration maps. */
final class ChannelConfigs {

  private ChannelConfigs() {}

  static String string(Map<String, Object> config, String key) {
    if (config == null) {
      return null;
    }
    Object value = config.get(key);
    return value == null ? null : value.toString();
  }
}
