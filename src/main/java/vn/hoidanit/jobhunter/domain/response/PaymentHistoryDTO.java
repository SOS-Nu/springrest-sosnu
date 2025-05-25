package vn.hoidanit.jobhunter.domain.response;

import java.time.LocalDateTime;
import vn.hoidanit.jobhunter.domain.PaymentHistory.PaymentStatus;

public class PaymentHistoryDTO {
    private Long userId;
    private Long amount;
    private String orderId;
    private String responseCode;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    public PaymentHistoryDTO(Long userId, Long amount, String orderId, String responseCode, PaymentStatus status,
            LocalDateTime createdAt) {
        this.userId = userId;
        this.amount = amount;
        this.orderId = orderId;
        this.responseCode = responseCode;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}