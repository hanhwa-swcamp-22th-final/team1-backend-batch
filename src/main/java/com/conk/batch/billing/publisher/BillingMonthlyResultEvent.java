package com.conk.batch.billing.publisher;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.domain.SellerMonthlyBilling;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * seller별 월 정산 결과 이벤트 페이로드다.
 */
public record BillingMonthlyResultEvent(
        String billingMonth,
        String sellerId,
        String warehouseId,
        Integer occupiedBinDays,
        BigDecimal averageOccupiedBins,
        BigDecimal storageUnitPrice,
        BigDecimal storageFee,
        Integer pickCount,
        BigDecimal pickUnitPrice,
        BigDecimal pickingFee,
        Integer packCount,
        BigDecimal packUnitPrice,
        BigDecimal packingFee,
        BigDecimal totalFee,
        LocalDateTime calculatedAt,
        Integer version
) {

    public static BillingMonthlyResultEvent from(
            SellerMonthlyBilling billing,
            MonthlyFeeSnapshot feeSnapshot
    ) {
        return new BillingMonthlyResultEvent(
                billing.getBillingMonth(),
                billing.getSellerId(),
                billing.getWarehouseId(),
                billing.getOccupiedBinDays(),
                billing.getAverageOccupiedBins(),
                feeSnapshot.getStorageUnitPrice(),
                billing.getStorageFee(),
                billing.getPickCount(),
                feeSnapshot.getPickUnitPrice(),
                billing.getPickingFee(),
                billing.getPackCount(),
                feeSnapshot.getPackUnitPrice(),
                billing.getPackingFee(),
                billing.getTotalFee(),
                billing.getCalculatedAt(),
                1
        );
    }

    public String kafkaKey() {
        return billingMonth + ":" + sellerId;
    }
}
