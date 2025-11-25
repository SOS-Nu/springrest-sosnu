package vn.hoidanit.jobhunter.domain.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.PaymentHistory.PaymentStatus;

@Getter
@Setter
public class PaymentHistoryDTO {
    private Long id;
    private String userEmail; // <--- Thêm Email của User
    private Long userId;
    private Long amount;
    private String orderId;
    private String responseCode;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public PaymentHistoryDTO(Long id, String userEmail, Long userId, Long amount, String orderId, String responseCode,
            PaymentStatus status,
            LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.userId = userId;
        this.amount = amount;
        this.orderId = orderId;
        this.responseCode = responseCode;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.id = id;
        this.userEmail = userEmail;

    }

}