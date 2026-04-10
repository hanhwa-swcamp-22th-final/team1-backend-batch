package com.conk.batch.billing.service;

import com.conk.batch.billing.client.WmsBillingQueryClient;
import com.conk.batch.billing.domain.DailyBinSnapshot;
import com.conk.batch.billing.repository.DailyBinSnapshotRepository;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일별 occupied bin 스냅샷을 저장한다.
 */
@Service
@RequiredArgsConstructor
public class DailyBinSnapshotService {

    private final WmsBillingQueryClient wmsBillingQueryClient;
    private final DailyBinSnapshotRepository dailyBinSnapshotRepository;

    @Transactional
    public List<DailyBinSnapshot> captureDailySnapshots(LocalDate baseDate) {
        if (baseDate == null) {
            throw new BusinessException(BatchErrorCode.INVALID_SNAPSHOT_DATE, "baseDate must not be null");
        }

        List<DailyBinSnapshot> snapshots;
        try {
            snapshots = wmsBillingQueryClient.getBinCountSummaries(baseDate).stream()
                    .map(response -> DailyBinSnapshot.of(
                            baseDate,
                            response.sellerId(),
                            response.warehouseId(),
                            response.occupiedBinCount()
                    ))
                    .toList();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    BatchErrorCode.WMS_BIN_COUNT_FETCH_FAILED,
                    "failed to fetch daily bin counts from WMS for baseDate=" + baseDate,
                    exception
            );
        }

        dailyBinSnapshotRepository.deleteBySnapshotDate(baseDate);
        dailyBinSnapshotRepository.flush();

        return dailyBinSnapshotRepository.saveAllAndFlush(snapshots);
    }
}
