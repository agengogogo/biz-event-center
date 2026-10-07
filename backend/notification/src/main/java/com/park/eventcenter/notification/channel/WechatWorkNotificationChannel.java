package com.park.eventcenter.notification.channel;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** WeChat Work (企业微信) group robot channel posting a markdown message. */
@Component
public class WechatWorkNotificationChannel implements NotificationChannel {

  public static final String TYPE = "WECHAT_WORK";

  private final RestClient.Builder restClientBuilder;

  public WechatWorkNotificationChannel(RestClient.Builder restClientBuilder) {
    this.restClientBuilder = restClientBuilder;
  }

  @Override
  public String type() {
    return TYPE;
  }

  @Override
  public DeliveryResult send(NotificationMessage message) {
    String url = ChannelConfigs.string(message.config(), "url");
    if (url == null || url.isBlank()) {
      return DeliveryResult.failure("WeChat Work channel is missing 'url' configuration");
    }
    String content = message.content() == null ? "" : message.content();
    Map<String, Object> body =
        Map.of("msgtype", "markdown", "markdown", Map.of("content", content));
    try {
      restClientBuilder
          .build()
          .post()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .toBodilessEntity();
      return DeliveryResult.ok();
    } catch (RuntimeException ex) {
      return DeliveryResult.failure(ex.getMessage());
    }
  }
}
