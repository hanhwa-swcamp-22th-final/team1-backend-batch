package com.conk.batch.billing.service;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.repository.MonthlyFeeSnapshotRepository;
import com.conk.batch.billing.repository.WmsBillingReadRepository;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기준월 단가 스냅샷을 캡처한다.
 */
@Service
@RequiredArgsConstructor
public class MonthlyFeeSnapshotService {

    private final MonthlyFeeSnapshotRepository monthlyFeeSnapshotRepository;
    private final WmsBillingReadRepository wmsBillingReadRepository;

    @Transactional
    public List<MonthlyFeeSnapshot> captureMonthlyFeeSnapshots(YearMonth billingMonth) {
        String billingMonthText = billingMonth.toString();
        monthlyFeeSnapshotRepository.deleteByBillingMonth(billingMonthText);

        List<MonthlyFeeSnapshot> snapshots = wmsBillingReadRepository.findFeeSettings(billingMonth).stream()
                .map(summary -> MonthlyFeeSnapshot.of(
                        billingMonthText,
                        summary.sellerId(),
                        summary.warehouseId(),
                        summary.storageUnitPrice(),
                        summary.pickUnitPrice(),
                        summary.packUnitPrice()
                ))
                .toList();

        return monthlyFeeSnapshotRepository.saveAll(snapshots);
    }
}
