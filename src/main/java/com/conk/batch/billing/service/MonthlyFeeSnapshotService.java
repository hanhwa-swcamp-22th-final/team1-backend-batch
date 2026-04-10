package com.conk.batch.billing.service;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.repository.MonthlyFeeSnapshotRepository;
import com.conk.batch.billing.repository.WmsBillingReadRepository;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
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
        if (billingMonth == null) {
            throw new BusinessException(BatchErrorCode.INVALID_BILLING_MONTH, "billingMonth must not be null");
        }

        String billingMonthText = billingMonth.toString();
        List<MonthlyFeeSnapshot> snapshots;
        try {
            snapshots = wmsBillingReadRepository.findFeeSettings(billingMonth).stream()
                    .map(summary -> MonthlyFeeSnapshot.of(
                            billingMonthText,
                            summary.sellerId(),
                            summary.warehouseId(),
                            summary.storageUnitPrice(),
                            summary.pickUnitPrice(),
                            summary.packUnitPrice()
                    ))
                    .toList();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    BatchErrorCode.FEE_SETTING_FETCH_FAILED,
                    "failed to fetch fee settings from WMS for billingMonth=" + billingMonthText,
                    exception
            );
        }

        monthlyFeeSnapshotRepository.deleteByBillingMonth(billingMonthText);
        monthlyFeeSnapshotRepository.flush();

        return monthlyFeeSnapshotRepository.saveAllAndFlush(snapshots);
    }
}
