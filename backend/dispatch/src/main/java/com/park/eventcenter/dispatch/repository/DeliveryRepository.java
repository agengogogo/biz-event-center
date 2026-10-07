package com.park.eventcenter.dispatch.repository;

import com.park.eventcenter.dispatch.entity.DeliveryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Delivery attempt persistence. */
public interface DeliveryRepository extends JpaRepository<DeliveryEntity, Long> {

  Optional<DeliveryEntity> findBySubscriptionIdAndEventId(String subscriptionId, String eventId);

  List<DeliveryEntity> findByParkIdOrderByCreatedAtDesc(String parkId);

  List<DeliveryEntity> findByParkIdAndDeadLetterTrueOrderByCreatedAtDesc(String parkId);

  List<DeliveryEntity> findByEventIdAndParkIdOrderByCreatedAtDesc(String eventId, String parkId);

  Optional<DeliveryEntity> findByIdAndParkId(Long id, String parkId);
}
