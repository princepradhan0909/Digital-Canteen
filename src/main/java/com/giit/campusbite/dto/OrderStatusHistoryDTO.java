
package com.giit.campusbite.dto;

import java.time.LocalDateTime;

public class OrderStatusHistoryDTO {

    private Long historyId;
    private Long orderId;
    private String status;
    private LocalDateTime changedAt;

    public OrderStatusHistoryDTO() {
    }

    public OrderStatusHistoryDTO(
            Long historyId,
            Long orderId,
            String status,
            LocalDateTime changedAt) {

        this.historyId = historyId;
        this.orderId = orderId;
        this.status = status;
        this.changedAt = changedAt;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
