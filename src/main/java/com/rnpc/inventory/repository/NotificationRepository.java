package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByAudienceOrderByCreatedAtDesc(Notification.Audience audience);

    List<Notification> findByUser_UsernameOrderByCreatedAtDesc(String username);

    long countByAudienceAndIsReadFalse(Notification.Audience audience);

    long countByUser_UsernameAndIsReadFalse(String username);
}
