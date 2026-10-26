package com.giit.campusbite;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final OrderNotificationRepository notifications;

    public NotificationService(OrderNotificationRepository notifications) {
        this.notifications = notifications;
    }

    public void notifyOrder(CustomerOrder order, String message) {
        if (order.getStudentEmail() == null || order.getStudentEmail().isBlank()) return;
        notifications.save(new OrderNotification(
                order.getStudentEmail(), order.getId(), message));
    }

    public List<OrderNotification> listFor(String email) {
        return notifications.findTop50ByRecipientEmailOrderByCreatedAtDesc(email);
    }

    @Transactional
    public OrderNotification markRead(String email, Long notificationId) {
        OrderNotification notification = notifications
                .findByIdAndRecipientEmail(notificationId, email)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return notification;
    }
}
