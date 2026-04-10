package com.conk.batch.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 셀러별 월 정산 결과다.
 */
@Getter
@Entity
@Table(
        name = "seller_monthly_billing",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seller_monthly_billing_month_seller_warehouse",
                columnNames = {"billing_month", "seller_id", "warehouse_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerMonthlyBilling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seller_monthly_billing_id", nullable = false)
    private Long id;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "seller_id", nullable = false, length = 50)
    private String sellerId;

    @Column(name = "warehouse_id", nullable = false, length = 50)
    private String warehouseId;

    @Column(name = "occupied_bin_days", nullable = false)
    private Integer occupiedBinDays;

    @Column(name = "average_occupied_bins", nullable = false, precision = 19, scale = 2)
    private BigDecimal averageOccupiedBins;

    @Column(name = "storage_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal storageFee;

    @Column(name = "pick_count", nullable = false)
    private Integer pickCount;

    @Column(name = "picking_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal pickingFee;

    @Column(name = "pack_count", nullable = false)
    private Integer packCount;

    @Column(name = "packing_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal packingFee;

    @Column(name = "total_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BillingStatus status;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    public static SellerMonthlyBilling calculated(
            String billingMonth,
            String sellerId,
            String warehouseId,
            Integer occupiedBinDays,
            BigDecimal averageOccupiedBins,
            BigDecimal storageFee,
            Integer pickCount,
            BigDecimal pickingFee,
            Integer packCount,
            BigDecimal packingFee
    ) {
        SellerMonthlyBilling billing = new SellerMonthlyBilling();
        billing.billingMonth = billingMonth;
        billing.sellerId = sellerId;
        billing.warehouseId = warehouseId;
        billing.occupiedBinDays = occupiedBinDays;
        billing.averageOccupiedBins = averageOccupiedBins;
        billing.storageFee = storageFee;
        billing.pickCount = pickCount;
        billing.pickingFee = pickingFee;
        billing.packCount = packCount;
        billing.packingFee = packingFee;
        billing.totalFee = storageFee.add(pickingFee).add(packingFee);
        billing.status = BillingStatus.CALCULATED;
        billing.calculatedAt = LocalDateTime.now();
        return billing;
    }

    public void markDispatched() {
        this.status = BillingStatus.DISPATCHED;
    }

    public void markPublishFailed() {
        this.status = BillingStatus.PUBLISH_FAILED;
    }
}
