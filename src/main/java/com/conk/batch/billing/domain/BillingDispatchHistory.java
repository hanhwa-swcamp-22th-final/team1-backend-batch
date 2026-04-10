package com.conk.batch.billing.domain;

import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 월 정산 결과 발행 이력을 저장한다.
 */
@Getter
@Entity
@Table(name = "billing_dispatch_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingDispatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_dispatch_history_id", nullable = false)
    private Long id;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "seller_id", nullable = false, length = 50)
    private String sellerId;

    @Column(name = "topic_name", nullable = false, length = 100)
    private String topicName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status", nullable = false, length = 20)
    private BillingDispatchStatus dispatchStatus;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    public static BillingDispatchHistory pending(
            String billingMonth,
            String sellerId,
            String topicName
    ) {
        validatePending(billingMonth, sellerId, topicName);

        BillingDispatchHistory history = new BillingDispatchHistory();
        history.billingMonth = billingMonth;
        history.sellerId = sellerId;
        history.topicName = topicName;
        history.dispatchStatus = BillingDispatchStatus.PENDING;
        return history;
    }

    public void markSuccess() {
        this.dispatchStatus = BillingDispatchStatus.SUCCESS;
        this.dispatchedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.dispatchStatus = BillingDispatchStatus.FAILED;
        this.dispatchedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    private static void validatePending(String billingMonth, String sellerId, String topicName) {
        if (billingMonth == null || billingMonth.isBlank()) {
            throw new BusinessException(BatchErrorCode.INVALID_BILLING_MONTH, "billingMonth must not be blank");
        }
        if (sellerId == null || sellerId.isBlank()) {
            throw new BusinessException(BatchErrorCode.INVALID_SELLER_ID, "sellerId must not be blank");
        }
        if (topicName == null || topicName.isBlank()) {
            throw new BusinessException(BatchErrorCode.INVALID_TOPIC_NAME, "topicName must not be blank");
        }
    }
}
