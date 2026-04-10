package com.conk.batch.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conk.batch.billing.client.WmsBillingQueryClient;
import com.conk.batch.billing.client.dto.BinCountSummaryResponse;
import com.conk.batch.billing.domain.DailyBinSnapshot;
import com.conk.batch.billing.repository.DailyBinSnapshotRepository;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyBinSnapshotServiceTest {

    @Mock
    private WmsBillingQueryClient wmsBillingQueryClient;

    @Mock
    private DailyBinSnapshotRepository dailyBinSnapshotRepository;

    @InjectMocks
    private DailyBinSnapshotService dailyBinSnapshotService;

    @Test
    @DisplayName("일별 bin snapshot 저장 성공: WMS 응답을 snapshot 엔티티로 변환해 저장한다")
    @SuppressWarnings("unchecked")
    void captureDailySnapshots_success() {
        LocalDate baseDate = LocalDate.of(2026, 4, 10);
        when(wmsBillingQueryClient.getBinCountSummaries(baseDate)).thenReturn(List.of(
                new BinCountSummaryResponse("SELLER-001", "WH-001", 3),
                new BinCountSummaryResponse("SELLER-002", "WH-001", 1)
        ));
        when(dailyBinSnapshotRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<DailyBinSnapshot> result = dailyBinSnapshotService.captureDailySnapshots(baseDate);

        ArgumentCaptor<List<DailyBinSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyBinSnapshotRepository).deleteBySnapshotDate(baseDate);
        verify(dailyBinSnapshotRepository).flush();
        verify(dailyBinSnapshotRepository).saveAllAndFlush(captor.capture());

        List<DailyBinSnapshot> saved = captor.getValue();
        assertEquals(2, result.size());
        assertEquals(2, saved.size());
        assertEquals("SELLER-001", saved.get(0).getSellerId());
        assertEquals(3, saved.get(0).getOccupiedBinCount());
        assertEquals("SELLER-002", saved.get(1).getSellerId());
        assertEquals(1, saved.get(1).getOccupiedBinCount());
    }

    @Test
    @DisplayName("일별 bin snapshot 저장 성공: WMS 응답이 비어 있으면 빈 목록을 저장한다")
    @SuppressWarnings("unchecked")
    void captureDailySnapshots_whenWmsReturnsEmpty_thenSaveEmptyList() {
        LocalDate baseDate = LocalDate.of(2026, 4, 10);
        when(wmsBillingQueryClient.getBinCountSummaries(baseDate)).thenReturn(List.of());
        when(dailyBinSnapshotRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<DailyBinSnapshot> result = dailyBinSnapshotService.captureDailySnapshots(baseDate);

        ArgumentCaptor<List<DailyBinSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyBinSnapshotRepository).saveAllAndFlush(captor.capture());
        assertTrue(result.isEmpty());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    @DisplayName("일별 bin snapshot 저장 실패: WMS 조회 중 예외가 발생하면 삭제와 저장을 수행하지 않고 예외를 전파한다")
    void captureDailySnapshots_whenWmsClientThrows_thenPropagateWithoutDeletingOrSaving() {
        LocalDate baseDate = LocalDate.of(2026, 4, 10);
        when(wmsBillingQueryClient.getBinCountSummaries(baseDate))
                .thenThrow(new IllegalStateException("WMS unavailable"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> dailyBinSnapshotService.captureDailySnapshots(baseDate));

        assertEquals(BatchErrorCode.WMS_BIN_COUNT_FETCH_FAILED, exception.getErrorCode());
        assertEquals("failed to fetch daily bin counts from WMS for baseDate=2026-04-10", exception.getMessage());
        verify(dailyBinSnapshotRepository, never()).deleteBySnapshotDate(any());
        verify(dailyBinSnapshotRepository, never()).flush();
        verify(dailyBinSnapshotRepository, never()).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("일별 bin snapshot 저장 실패: WMS 응답에 잘못된 창고 코드가 있으면 삭제와 저장을 수행하지 않고 예외를 전파한다")
    void captureDailySnapshots_whenWmsResponseContainsBlankWarehouse_thenPropagateWithoutDeletingOrSaving() {
        LocalDate baseDate = LocalDate.of(2026, 4, 10);
        when(wmsBillingQueryClient.getBinCountSummaries(baseDate)).thenReturn(List.of(
                new BinCountSummaryResponse("SELLER-001", " ", 3)
        ));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> dailyBinSnapshotService.captureDailySnapshots(baseDate));

        assertEquals(BatchErrorCode.INVALID_WAREHOUSE_ID, exception.getErrorCode());
        assertEquals("warehouseId must not be blank", exception.getMessage());
        verify(dailyBinSnapshotRepository, never()).deleteBySnapshotDate(any());
        verify(dailyBinSnapshotRepository, never()).flush();
        verify(dailyBinSnapshotRepository, never()).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("일별 bin snapshot 저장 실패: 기준 날짜가 없으면 WMS 조회와 저장을 수행하지 않고 예외를 전파한다")
    void captureDailySnapshots_whenBaseDateIsNull_thenPropagateWithoutCallingDependencies() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> dailyBinSnapshotService.captureDailySnapshots(null));

        assertEquals(BatchErrorCode.INVALID_SNAPSHOT_DATE, exception.getErrorCode());
        assertEquals("baseDate must not be null", exception.getMessage());
        verify(wmsBillingQueryClient, never()).getBinCountSummaries(any());
        verify(dailyBinSnapshotRepository, never()).deleteBySnapshotDate(any());
        verify(dailyBinSnapshotRepository, never()).flush();
        verify(dailyBinSnapshotRepository, never()).saveAllAndFlush(any());
    }
}
