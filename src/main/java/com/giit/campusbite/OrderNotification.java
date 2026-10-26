package com.giit.campusbite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_notifications")
public class OrderNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime readAt;

    protected OrderNotification() {
    }

    public OrderNotification(String recipientEmail, Long orderId, String message) {
        this.recipientEmail = recipientEmail;
        this.orderId = orderId;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public Long getOrderId() { return orderId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
