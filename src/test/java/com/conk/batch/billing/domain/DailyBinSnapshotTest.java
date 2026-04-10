package com.conk.batch.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DailyBinSnapshotTest {

    @Test
    @DisplayName("일별 bin snapshot 생성 성공: 날짜, 셀러, 창고, 점유 bin 수가 올바르게 설정된다")
    void create_success() {
        DailyBinSnapshot snapshot = DailyBinSnapshot.of(
                LocalDate.of(2026, 4, 10),
                "SELLER-001",
                "WH-001",
                3
        );

        assertEquals(LocalDate.of(2026, 4, 10), snapshot.getSnapshotDate());
        assertEquals("SELLER-001", snapshot.getSellerId());
        assertEquals("WH-001", snapshot.getWarehouseId());
        assertEquals(3, snapshot.getOccupiedBinCount());
        assertNotNull(snapshot.getCreatedAt());
    }

    @Test
    @DisplayName("일별 bin snapshot 생성 실패: 점유 bin 수가 음수면 예외가 발생한다")
    void create_whenOccupiedBinCountIsNegative_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                DailyBinSnapshot.of(
                        LocalDate.of(2026, 4, 10),
                        "SELLER-001",
                        "WH-001",
                        -1
                )
        );

        assertEquals(BatchErrorCode.INVALID_OCCUPIED_BIN_COUNT, exception.getErrorCode());
        assertEquals("occupiedBinCount must be greater than or equal to 0", exception.getMessage());
    }

    @Test
    @DisplayName("일별 bin snapshot 생성 실패: sellerId가 비어 있으면 예외가 발생한다")
    void create_whenSellerIdIsBlank_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                DailyBinSnapshot.of(
                        LocalDate.of(2026, 4, 10),
                        " ",
                        "WH-001",
                        1
                )
        );

        assertEquals(BatchErrorCode.INVALID_SELLER_ID, exception.getErrorCode());
        assertEquals("sellerId must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("일별 bin snapshot 생성 실패: snapshotDate가 없으면 예외가 발생한다")
    void create_whenSnapshotDateIsNull_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                DailyBinSnapshot.of(
                        null,
                        "SELLER-001",
                        "WH-001",
                        1
                )
        );

        assertEquals(BatchErrorCode.INVALID_SNAPSHOT_DATE, exception.getErrorCode());
        assertEquals("snapshotDate must not be null", exception.getMessage());
    }
}
