package com.park.eventcenter.notification.repository;

import com.park.eventcenter.notification.entity.NotificationDeliveryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Notification delivery record persistence. */
public interface NotificationDeliveryRepository
    extends JpaRepository<NotificationDeliveryEntity, Long> {

  List<NotificationDeliveryEntity> findByParkIdOrderByCreatedAtDesc(String parkId);

  List<NotificationDeliveryEntity> findByEventIdAndParkIdOrderByCreatedAtDesc(
      String eventId, String parkId);

  Optional<NotificationDeliveryEntity> findByIdAndParkId(Long id, String parkId);
}
