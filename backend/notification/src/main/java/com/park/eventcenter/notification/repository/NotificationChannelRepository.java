package com.park.eventcenter.notification.repository;

import com.park.eventcenter.notification.entity.NotificationChannelEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Notification channel persistence. */
public interface NotificationChannelRepository
    extends JpaRepository<NotificationChannelEntity, String> {

  List<NotificationChannelEntity> findByParkIdOrderByNameAsc(String parkId);

  Optional<NotificationChannelEntity> findByIdAndParkId(String id, String parkId);
}
