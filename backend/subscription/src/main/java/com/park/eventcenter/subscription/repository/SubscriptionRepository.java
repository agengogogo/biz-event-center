package com.park.eventcenter.subscription.repository;

import com.park.eventcenter.subscription.entity.SubscriptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Subscription persistence scoped by park. */
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {

  List<SubscriptionEntity> findByParkIdOrderByCreatedAtDesc(String parkId);

  List<SubscriptionEntity> findByParkIdAndEnabledTrue(String parkId);

  Optional<SubscriptionEntity> findByIdAndParkId(String id, String parkId);
}
