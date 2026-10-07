package com.park.eventcenter.dispatch.delivery;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resolves a delivery target implementation for a subscription's target type. */
@Component
public class DeliveryTargetRegistry {

  private final List<DeliveryTarget> targets;

  public DeliveryTargetRegistry(List<DeliveryTarget> targets) {
    this.targets = targets;
  }

  public Optional<DeliveryTarget> find(String type) {
    return targets.stream().filter(target -> target.supports(type)).findFirst();
  }
}
