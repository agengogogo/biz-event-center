package com.park.eventcenter.dispatch.delivery;

import com.park.eventcenter.notification.channel.DeliveryResult;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Delivers events to generic HTTP/Webhook targets. */
@Component
public class WebhookDeliveryTarget implements DeliveryTarget {

  public static final String WEBHOOK = "WEBHOOK";
  public static final String HTTP = "HTTP";

  private final RestClient.Builder restClientBuilder;

  public WebhookDeliveryTarget(RestClient.Builder restClientBuilder) {
    this.restClientBuilder = restClientBuilder;
  }

  @Override
  public boolean supports(String type) {
    return type != null && (WEBHOOK.equalsIgnoreCase(type) || HTTP.equalsIgnoreCase(type));
  }

  @Override
  public DeliveryResult deliver(DeliveryRequest request) {
    if (request.targetUrl() == null || request.targetUrl().isBlank()) {
      return DeliveryResult.failure("Delivery target url is not configured");
    }
    Map<String, Object> body = Map.of("event", request.event());
    try {
      restClientBuilder
          .build()
          .post()
          .uri(request.targetUrl())
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
