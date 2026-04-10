package com.conk.batch.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MonthlyFeeSnapshotTest {

    @Test
    @DisplayName("월별 요금 snapshot 생성 성공: 기준월, 셀러, 창고, 단가가 올바르게 설정된다")
    void create_success() {
        MonthlyFeeSnapshot snapshot = MonthlyFeeSnapshot.of(
                "2026-03",
                "SELLER-001",
                "WH-001",
                new BigDecimal("28500"),
                new BigDecimal("2500"),
                new BigDecimal("2500")
        );

        assertEquals("2026-03", snapshot.getBillingMonth());
        assertEquals("SELLER-001", snapshot.getSellerId());
        assertEquals("WH-001", snapshot.getWarehouseId());
        assertEquals(new BigDecimal("28500"), snapshot.getStorageUnitPrice());
        assertEquals(new BigDecimal("2500"), snapshot.getPickUnitPrice());
        assertEquals(new BigDecimal("2500"), snapshot.getPackUnitPrice());
        assertNotNull(snapshot.getCapturedAt());
    }

    @Test
    @DisplayName("월별 요금 snapshot 생성 실패: 기준월이 비어 있으면 예외가 발생한다")
    void create_whenBillingMonthIsBlank_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                MonthlyFeeSnapshot.of(
                        " ",
                        "SELLER-001",
                        "WH-001",
                        new BigDecimal("28500"),
                        new BigDecimal("2500"),
                        new BigDecimal("2500")
                )
        );

        assertEquals(BatchErrorCode.INVALID_BILLING_MONTH, exception.getErrorCode());
        assertEquals("billingMonth must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("월별 요금 snapshot 생성 실패: storage 단가가 음수면 예외가 발생한다")
    void create_whenStorageUnitPriceIsNegative_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                MonthlyFeeSnapshot.of(
                        "2026-03",
                        "SELLER-001",
                        "WH-001",
                        new BigDecimal("-1"),
                        new BigDecimal("2500"),
                        new BigDecimal("2500")
                )
        );

        assertEquals(BatchErrorCode.INVALID_STORAGE_UNIT_PRICE, exception.getErrorCode());
        assertEquals("storageUnitPrice must be greater than or equal to 0", exception.getMessage());
    }

    @Test
    @DisplayName("월별 요금 snapshot 생성 실패: warehouseId가 비어 있으면 예외가 발생한다")
    void create_whenWarehouseIdIsBlank_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                MonthlyFeeSnapshot.of(
                        "2026-03",
                        "SELLER-001",
                        " ",
                        new BigDecimal("28500"),
                        new BigDecimal("2500"),
                        new BigDecimal("2500")
                )
        );

        assertEquals(BatchErrorCode.INVALID_WAREHOUSE_ID, exception.getErrorCode());
        assertEquals("warehouseId must not be blank", exception.getMessage());
    }
}
