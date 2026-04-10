package com.conk.batch.billing.service;

import com.conk.batch.billing.client.WmsBillingQueryClient;
import com.conk.batch.billing.domain.DailyBinSnapshot;
import com.conk.batch.billing.repository.DailyBinSnapshotRepository;
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
        dailyBinSnapshotRepository.deleteBySnapshotDate(baseDate);

        List<DailyBinSnapshot> snapshots = wmsBillingQueryClient.getBinCountSummaries(baseDate).stream()
                .map(response -> DailyBinSnapshot.of(
                        baseDate,
                        response.sellerId(),
                        response.warehouseId(),
                        response.occupiedBinCount()
                ))
                .toList();

        return dailyBinSnapshotRepository.saveAll(snapshots);
    }
}
