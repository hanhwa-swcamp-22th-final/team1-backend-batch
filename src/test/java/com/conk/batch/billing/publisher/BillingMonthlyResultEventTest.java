package com.conk.batch.billing.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.domain.SellerMonthlyBilling;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingMonthlyResultEventTest {

    @Test
    @DisplayName("월 정산 결과 이벤트 생성 성공: billing과 fee snapshot 값을 payload와 key로 변환한다")
    void from_success() {
        SellerMonthlyBilling billing = SellerMonthlyBilling.calculated(
                "2026-03",
                "SELLER-001",
                "WH-001",
                87,
                new BigDecimal("2.81"),
                new BigDecimal("80085"),
                3,
                new BigDecimal("7500"),
                1,
                new BigDecimal("2500")
        );
        MonthlyFeeSnapshot feeSnapshot = MonthlyFeeSnapshot.of(
                "2026-03",
                "SELLER-001",
                "WH-001",
                new BigDecimal("28500"),
                new BigDecimal("2500"),
                new BigDecimal("2500")
        );

        BillingMonthlyResultEvent event = BillingMonthlyResultEvent.from(billing, feeSnapshot);

        assertEquals("2026-03", event.billingMonth());
        assertEquals("SELLER-001", event.sellerId());
        assertEquals("WH-001", event.warehouseId());
        assertEquals(87, event.occupiedBinDays());
        assertEquals(new BigDecimal("2.81"), event.averageOccupiedBins());
        assertEquals(new BigDecimal("28500"), event.storageUnitPrice());
        assertEquals(new BigDecimal("80085"), event.storageFee());
        assertEquals(3, event.pickCount());
        assertEquals(new BigDecimal("2500"), event.pickUnitPrice());
        assertEquals(new BigDecimal("7500"), event.pickingFee());
        assertEquals(1, event.packCount());
        assertEquals(new BigDecimal("2500"), event.packUnitPrice());
        assertEquals(new BigDecimal("2500"), event.packingFee());
        assertEquals(new BigDecimal("90085"), event.totalFee());
        assertEquals(1, event.version());
        assertEquals("2026-03:SELLER-001", event.kafkaKey());
    }
}
