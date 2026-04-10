package com.conk.batch.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SellerMonthlyBillingTest {

    @Test
    @DisplayName("월 정산 결과 생성 성공: 계산 금액과 상태가 올바르게 설정된다")
    void calculated_success() {
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

        assertEquals("2026-03", billing.getBillingMonth());
        assertEquals("SELLER-001", billing.getSellerId());
        assertEquals("WH-001", billing.getWarehouseId());
        assertEquals(87, billing.getOccupiedBinDays());
        assertEquals(new BigDecimal("2.81"), billing.getAverageOccupiedBins());
        assertEquals(new BigDecimal("80085"), billing.getStorageFee());
        assertEquals(3, billing.getPickCount());
        assertEquals(new BigDecimal("7500"), billing.getPickingFee());
        assertEquals(1, billing.getPackCount());
        assertEquals(new BigDecimal("2500"), billing.getPackingFee());
        assertEquals(new BigDecimal("90085"), billing.getTotalFee());
        assertEquals(BillingStatus.CALCULATED, billing.getStatus());
        assertNotNull(billing.getCalculatedAt());
    }

    @Test
    @DisplayName("월 정산 결과 생성 실패: occupiedBinDays가 음수면 예외가 발생한다")
    void calculated_whenOccupiedBinDaysIsNegative_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                SellerMonthlyBilling.calculated(
                        "2026-03",
                        "SELLER-001",
                        "WH-001",
                        -1,
                        new BigDecimal("2.81"),
                        new BigDecimal("80085"),
                        3,
                        new BigDecimal("7500"),
                        1,
                        new BigDecimal("2500")
                )
        );

        assertEquals(BatchErrorCode.INVALID_OCCUPIED_BIN_DAYS, exception.getErrorCode());
        assertEquals("occupiedBinDays must be greater than or equal to 0", exception.getMessage());
    }

    @Test
    @DisplayName("월 정산 결과 생성 실패: pickCount가 음수면 예외가 발생한다")
    void calculated_whenPickCountIsNegative_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                SellerMonthlyBilling.calculated(
                        "2026-03",
                        "SELLER-001",
                        "WH-001",
                        87,
                        new BigDecimal("2.81"),
                        new BigDecimal("80085"),
                        -1,
                        new BigDecimal("7500"),
                        1,
                        new BigDecimal("2500")
                )
        );

        assertEquals(BatchErrorCode.INVALID_PICK_COUNT, exception.getErrorCode());
        assertEquals("pickCount must be greater than or equal to 0", exception.getMessage());
    }

    @Test
    @DisplayName("월 정산 결과 상태 변경 성공: 발행 성공 시 DISPATCHED로 변경된다")
    void markDispatched_success() {
        SellerMonthlyBilling billing = createBilling();

        billing.markDispatched();

        assertEquals(BillingStatus.DISPATCHED, billing.getStatus());
    }

    @Test
    @DisplayName("월 정산 결과 상태 변경 성공: 발행 실패 시 PUBLISH_FAILED로 변경된다")
    void markPublishFailed_success() {
        SellerMonthlyBilling billing = createBilling();

        billing.markPublishFailed();

        assertEquals(BillingStatus.PUBLISH_FAILED, billing.getStatus());
    }

    private SellerMonthlyBilling createBilling() {
        return SellerMonthlyBilling.calculated(
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
    }
}
