package com.conk.batch.billing.domain;

import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기준월에 적용되는 단가 스냅샷이다.
 */
@Getter
@Entity
@Table(
        name = "monthly_fee_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_fee_snapshot_month_seller_warehouse",
                columnNames = {"billing_month", "seller_id", "warehouse_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyFeeSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_fee_snapshot_id", nullable = false)
    private Long id;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "seller_id", nullable = false, length = 50)
    private String sellerId;

    @Column(name = "warehouse_id", nullable = false, length = 50)
    private String warehouseId;

    @Column(name = "storage_unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal storageUnitPrice;

    @Column(name = "pick_unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal pickUnitPrice;

    @Column(name = "pack_unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal packUnitPrice;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    public static MonthlyFeeSnapshot of(
            String billingMonth,
            String sellerId,
            String warehouseId,
            BigDecimal storageUnitPrice,
            BigDecimal pickUnitPrice,
            BigDecimal packUnitPrice
    ) {
        validate(billingMonth, sellerId, warehouseId, storageUnitPrice, pickUnitPrice, packUnitPrice);

        MonthlyFeeSnapshot snapshot = new MonthlyFeeSnapshot();
        snapshot.billingMonth = billingMonth;
        snapshot.sellerId = sellerId;
        snapshot.warehouseId = warehouseId;
        snapshot.storageUnitPrice = storageUnitPrice;
        snapshot.pickUnitPrice = pickUnitPrice;
        snapshot.packUnitPrice = packUnitPrice;
        snapshot.capturedAt = LocalDateTime.now();
        return snapshot;
    }

    private static void validate(
            String billingMonth,
            String sellerId,
            String warehouseId,
            BigDecimal storageUnitPrice,
            BigDecimal pickUnitPrice,
            BigDecimal packUnitPrice
    ) {
        if (billingMonth == null || billingMonth.isBlank()) {
            throw new BusinessException(BatchErrorCode.INVALID_BILLING_MONTH, "billingMonth must not be blank");
        }
        if (sellerId == null || sellerId.isBlank()) {
            throw new BusinessException(BatchErrorCode.INVALID_SELLER_ID, "sellerId must not be blank");
        }
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new BusinessException(BatchErrorCode.INVALID_WAREHOUSE_ID, "warehouseId must not be blank");
        }
        validateNonNegative(storageUnitPrice, BatchErrorCode.INVALID_STORAGE_UNIT_PRICE, "storageUnitPrice");
        validateNonNegative(pickUnitPrice, BatchErrorCode.INVALID_PICK_UNIT_PRICE, "pickUnitPrice");
        validateNonNegative(packUnitPrice, BatchErrorCode.INVALID_PACK_UNIT_PRICE, "packUnitPrice");
    }

    private static void validateNonNegative(
            BigDecimal value,
            BatchErrorCode errorCode,
            String fieldName
    ) {
        if (value == null) {
            throw new BusinessException(errorCode, fieldName + " must not be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(errorCode, fieldName + " must be greater than or equal to 0");
        }
    }
}
