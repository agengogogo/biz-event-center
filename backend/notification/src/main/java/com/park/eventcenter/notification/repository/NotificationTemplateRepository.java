package com.park.eventcenter.notification.repository;

import com.park.eventcenter.notification.entity.NotificationTemplateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Notification template persistence. */
public interface NotificationTemplateRepository
    extends JpaRepository<NotificationTemplateEntity, String> {

  List<NotificationTemplateEntity> findByParkIdOrderByNameAsc(String parkId);

  Optional<NotificationTemplateEntity> findByIdAndParkId(String id, String parkId);
}
