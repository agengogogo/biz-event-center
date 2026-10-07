package com.park.eventcenter.notification.channel;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Generic JSON webhook channel: POSTs the rendered content to a configured URL. */
@Component
public class WebhookNotificationChannel implements NotificationChannel {

  public static final String TYPE = "WEBHOOK";

  private final RestClient.Builder restClientBuilder;

  public WebhookNotificationChannel(RestClient.Builder restClientBuilder) {
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
      return DeliveryResult.failure("Webhook channel is missing 'url' configuration");
    }
    Map<String, Object> body =
        Map.of(
            "subject", message.subject() == null ? "" : message.subject(),
            "content", message.content() == null ? "" : message.content(),
            "event", message.event());
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
