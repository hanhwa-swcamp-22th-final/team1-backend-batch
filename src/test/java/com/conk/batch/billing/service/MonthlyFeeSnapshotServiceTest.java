package com.conk.batch.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.repository.MonthlyFeeSnapshotRepository;
import com.conk.batch.billing.repository.WmsBillingReadRepository;
import com.conk.batch.billing.repository.dto.FeeSettingSummary;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlyFeeSnapshotServiceTest {

    @Mock
    private MonthlyFeeSnapshotRepository monthlyFeeSnapshotRepository;

    @Mock
    private WmsBillingReadRepository wmsBillingReadRepository;

    @InjectMocks
    private MonthlyFeeSnapshotService monthlyFeeSnapshotService;

    @Test
    @DisplayName("월별 요금 snapshot 저장 성공: WMS 요금 설정을 snapshot 엔티티로 변환해 저장한다")
    @SuppressWarnings("unchecked")
    void captureMonthlyFeeSnapshots_success() {
        YearMonth billingMonth = YearMonth.of(2026, 3);
        when(wmsBillingReadRepository.findFeeSettings(billingMonth)).thenReturn(List.of(
                new FeeSettingSummary("SELLER-001", "WH-001", new BigDecimal("28500"), new BigDecimal("2500"), new BigDecimal("2500")),
                new FeeSettingSummary("SELLER-002", "WH-001", new BigDecimal("27000"), new BigDecimal("2300"), new BigDecimal("2100"))
        ));
        when(monthlyFeeSnapshotRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<MonthlyFeeSnapshot> result = monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(billingMonth);

        ArgumentCaptor<List<MonthlyFeeSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(monthlyFeeSnapshotRepository).deleteByBillingMonth("2026-03");
        verify(monthlyFeeSnapshotRepository).flush();
        verify(monthlyFeeSnapshotRepository).saveAllAndFlush(captor.capture());

        List<MonthlyFeeSnapshot> saved = captor.getValue();
        assertEquals(2, result.size());
        assertEquals(2, saved.size());
        assertEquals("SELLER-001", saved.get(0).getSellerId());
        assertEquals(new BigDecimal("28500"), saved.get(0).getStorageUnitPrice());
        assertEquals("SELLER-002", saved.get(1).getSellerId());
        assertEquals(new BigDecimal("2100"), saved.get(1).getPackUnitPrice());
    }

    @Test
    @DisplayName("월별 요금 snapshot 저장 성공: WMS 요금 설정이 비어 있으면 빈 목록을 저장한다")
    @SuppressWarnings("unchecked")
    void captureMonthlyFeeSnapshots_whenWmsReturnsEmpty_thenSaveEmptyList() {
        YearMonth billingMonth = YearMonth.of(2026, 3);
        when(wmsBillingReadRepository.findFeeSettings(billingMonth)).thenReturn(List.of());
        when(monthlyFeeSnapshotRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<MonthlyFeeSnapshot> result = monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(billingMonth);

        ArgumentCaptor<List<MonthlyFeeSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(monthlyFeeSnapshotRepository).saveAllAndFlush(captor.capture());
        assertTrue(result.isEmpty());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    @DisplayName("월별 요금 snapshot 저장 실패: 기준월이 없으면 조회와 저장을 수행하지 않고 예외를 전파한다")
    void captureMonthlyFeeSnapshots_whenBillingMonthIsNull_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(null));

        assertEquals(BatchErrorCode.INVALID_BILLING_MONTH, exception.getErrorCode());
        assertEquals("billingMonth must not be null", exception.getMessage());
        verify(wmsBillingReadRepository, never()).findFeeSettings(any());
        verify(monthlyFeeSnapshotRepository, never()).deleteByBillingMonth(any());
        verify(monthlyFeeSnapshotRepository, never()).flush();
        verify(monthlyFeeSnapshotRepository, never()).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("월별 요금 snapshot 저장 실패: WMS 조회 예외가 발생하면 삭제와 저장을 수행하지 않고 예외를 래핑한다")
    void captureMonthlyFeeSnapshots_whenWmsReadThrows_thenWrapException() {
        YearMonth billingMonth = YearMonth.of(2026, 3);
        when(wmsBillingReadRepository.findFeeSettings(billingMonth))
                .thenThrow(new IllegalStateException("WMS fee settings unavailable"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(billingMonth));

        assertEquals(BatchErrorCode.FEE_SETTING_FETCH_FAILED, exception.getErrorCode());
        assertEquals("failed to fetch fee settings from WMS for billingMonth=2026-03", exception.getMessage());
        verify(monthlyFeeSnapshotRepository, never()).deleteByBillingMonth(any());
        verify(monthlyFeeSnapshotRepository, never()).flush();
        verify(monthlyFeeSnapshotRepository, never()).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("월별 요금 snapshot 저장 실패: WMS 응답에 잘못된 단가가 있으면 삭제와 저장을 수행하지 않고 예외를 전파한다")
    void captureMonthlyFeeSnapshots_whenWmsResponseContainsInvalidUnitPrice_thenPropagate() {
        YearMonth billingMonth = YearMonth.of(2026, 3);
        when(wmsBillingReadRepository.findFeeSettings(billingMonth)).thenReturn(List.of(
                new FeeSettingSummary("SELLER-001", "WH-001", new BigDecimal("-1"), new BigDecimal("2500"), new BigDecimal("2500"))
        ));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(billingMonth));

        assertEquals(BatchErrorCode.INVALID_STORAGE_UNIT_PRICE, exception.getErrorCode());
        assertEquals("storageUnitPrice must be greater than or equal to 0", exception.getMessage());
        verify(monthlyFeeSnapshotRepository, never()).deleteByBillingMonth(any());
        verify(monthlyFeeSnapshotRepository, never()).flush();
        verify(monthlyFeeSnapshotRepository, never()).saveAllAndFlush(any());
    }
}
