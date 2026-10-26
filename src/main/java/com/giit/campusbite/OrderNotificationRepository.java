package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderNotificationRepository
        extends JpaRepository<OrderNotification, Long> {
    List<OrderNotification> findTop50ByRecipientEmailOrderByCreatedAtDesc(
            String recipientEmail);
    Optional<OrderNotification> findByIdAndRecipientEmail(Long id, String recipientEmail);
}
