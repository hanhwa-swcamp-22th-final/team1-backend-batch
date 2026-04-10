package com.conk.batch.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.conk.batch.billing.client.WmsBillingQueryClient;
import com.conk.batch.billing.client.dto.BinCountSummaryResponse;
import com.conk.batch.billing.domain.DailyBinSnapshot;
import com.conk.batch.billing.repository.DailyBinSnapshotRepository;
import com.conk.batch.billing.service.DailyBinSnapshotService;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DailyBinSnapshotIntegrationTest {

    @Autowired
    private DailyBinSnapshotService dailyBinSnapshotService;

    @Autowired
    private DailyBinSnapshotRepository dailyBinSnapshotRepository;

    @MockitoBean
    private WmsBillingQueryClient wmsBillingQueryClient;

    @Test
    @DisplayName("일별 bin snapshot 저장 전체 흐름이 정상 동작한다")
    void captureDailySnapshots_flowSuccess() {
        LocalDate baseDate = LocalDate.of(2026, 4, 10);
        when(wmsBillingQueryClient.getBinCountSummaries(baseDate)).thenReturn(List.of(
                new BinCountSummaryResponse("SELLER-001", "WH-001", 3),
                new BinCountSummaryResponse("SELLER-002", "WH-001", 1)
        ));

        dailyBinSnapshotService.captureDailySnapshots(baseDate);

        List<DailyBinSnapshot> rows = dailyBinSnapshotRepository.findAll();
        assertThat(rows).hasSize(2);
        assertThat(rows)
                .extracting(DailyBinSnapshot::getSnapshotDate, DailyBinSnapshot::getSellerId, DailyBinSnapshot::getWarehouseId, DailyBinSnapshot::getOccupiedBinCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(baseDate, "SELLER-001", "WH-001", 3),
                        org.assertj.core.groups.Tuple.tuple(baseDate, "SELLER-002", "WH-001", 1)
                );
    }

    @Test
    @DisplayName("일별 bin snapshot 재적재 시 같은 날짜 데이터는 교체되고 다른 날짜 데이터는 유지된다")
    void captureDailySnapshots_whenRecaptured_thenReplaceOnlySameDateRows() {
        LocalDate baseDate = LocalDate.of(2026, 4, 10);
        dailyBinSnapshotRepository.save(DailyBinSnapshot.of(baseDate, "SELLER-001", "WH-001", 9));
        dailyBinSnapshotRepository.save(DailyBinSnapshot.of(LocalDate.of(2026, 4, 9), "SELLER-LEGACY", "WH-001", 2));

        when(wmsBillingQueryClient.getBinCountSummaries(baseDate)).thenReturn(List.of(
                new BinCountSummaryResponse("SELLER-001", "WH-001", 3),
                new BinCountSummaryResponse("SELLER-002", "WH-001", 1)
        ));

        dailyBinSnapshotService.captureDailySnapshots(baseDate);

        List<DailyBinSnapshot> rows = dailyBinSnapshotRepository.findAll();
        assertThat(rows)
                .extracting(DailyBinSnapshot::getSnapshotDate, DailyBinSnapshot::getSellerId, DailyBinSnapshot::getOccupiedBinCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(baseDate, "SELLER-001", 3),
                        org.assertj.core.groups.Tuple.tuple(baseDate, "SELLER-002", 1),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 4, 9), "SELLER-LEGACY", 2)
                );
    }

    @Test
    @DisplayName("일별 bin snapshot 저장 실패: WMS 조회에서 예외가 발생하면 기존 snapshot 데이터는 유지된다")
    void captureDailySnapshots_whenWmsClientThrows_thenKeepExistingRows() {
        LocalDate baseDate = LocalDate.of(2026, 4, 10);
        dailyBinSnapshotRepository.save(DailyBinSnapshot.of(baseDate, "SELLER-001", "WH-001", 9));
        dailyBinSnapshotRepository.save(DailyBinSnapshot.of(LocalDate.of(2026, 4, 9), "SELLER-LEGACY", "WH-001", 2));

        when(wmsBillingQueryClient.getBinCountSummaries(baseDate))
                .thenThrow(new IllegalStateException("WMS unavailable"));

        assertThatThrownBy(() -> dailyBinSnapshotService.captureDailySnapshots(baseDate))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(BatchErrorCode.WMS_BIN_COUNT_FETCH_FAILED);
                    assertThat(exception.getMessage()).isEqualTo("failed to fetch daily bin counts from WMS for baseDate=2026-04-10");
                });

        List<DailyBinSnapshot> rows = dailyBinSnapshotRepository.findAll();
        assertThat(rows)
                .extracting(DailyBinSnapshot::getSnapshotDate, DailyBinSnapshot::getSellerId, DailyBinSnapshot::getOccupiedBinCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(baseDate, "SELLER-001", 9),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 4, 9), "SELLER-LEGACY", 2)
                );
    }

    @Test
    @DisplayName("일별 bin snapshot 저장 실패: WMS 응답에 잘못된 데이터가 있으면 기존 snapshot 데이터는 유지된다")
    void captureDailySnapshots_whenWmsResponseIsInvalid_thenKeepExistingRows() {
        LocalDate baseDate = LocalDate.of(2026, 4, 10);
        dailyBinSnapshotRepository.save(DailyBinSnapshot.of(baseDate, "SELLER-001", "WH-001", 9));
        dailyBinSnapshotRepository.save(DailyBinSnapshot.of(LocalDate.of(2026, 4, 9), "SELLER-LEGACY", "WH-001", 2));

        when(wmsBillingQueryClient.getBinCountSummaries(baseDate)).thenReturn(List.of(
                new BinCountSummaryResponse("SELLER-001", " ", 3)
        ));

        assertThatThrownBy(() -> dailyBinSnapshotService.captureDailySnapshots(baseDate))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(BatchErrorCode.INVALID_WAREHOUSE_ID);
                    assertThat(exception.getMessage()).isEqualTo("warehouseId must not be blank");
                });

        List<DailyBinSnapshot> rows = dailyBinSnapshotRepository.findAll();
        assertThat(rows)
                .extracting(DailyBinSnapshot::getSnapshotDate, DailyBinSnapshot::getSellerId, DailyBinSnapshot::getOccupiedBinCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(baseDate, "SELLER-001", 9),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 4, 9), "SELLER-LEGACY", 2)
                );
    }

    @Test
    @DisplayName("일별 bin snapshot 저장 실패: 기준 날짜가 없으면 데이터베이스를 변경하지 않는다")
    void captureDailySnapshots_whenBaseDateIsNull_thenKeepExistingRows() {
        LocalDate existingDate = LocalDate.of(2026, 4, 9);
        dailyBinSnapshotRepository.save(DailyBinSnapshot.of(existingDate, "SELLER-LEGACY", "WH-001", 2));

        assertThatThrownBy(() -> dailyBinSnapshotService.captureDailySnapshots(null))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(BatchErrorCode.INVALID_SNAPSHOT_DATE);
                    assertThat(exception.getMessage()).isEqualTo("baseDate must not be null");
                });

        List<DailyBinSnapshot> rows = dailyBinSnapshotRepository.findAll();
        assertThat(rows)
                .extracting(DailyBinSnapshot::getSnapshotDate, DailyBinSnapshot::getSellerId, DailyBinSnapshot::getOccupiedBinCount)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(existingDate, "SELLER-LEGACY", 2));
    }
}
