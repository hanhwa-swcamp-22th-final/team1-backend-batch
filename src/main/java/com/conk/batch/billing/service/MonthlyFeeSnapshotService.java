package com.conk.batch.billing.service;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.repository.DailyBinSnapshotRepository;
import com.conk.batch.billing.repository.MonthlyFeeSnapshotRepository;
import com.conk.batch.billing.repository.WmsBillingReadRepository;
import com.conk.batch.billing.repository.dto.PickPackMonthlyAggregation;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기준월 단가 스냅샷을 캡처한다.
 */
@Service
@RequiredArgsConstructor
public class MonthlyFeeSnapshotService {

    private final DailyBinSnapshotRepository dailyBinSnapshotRepository;
    private final MonthlyFeeSnapshotRepository monthlyFeeSnapshotRepository;
    private final WmsBillingReadRepository wmsBillingReadRepository;

    @Transactional
    public List<MonthlyFeeSnapshot> captureMonthlyFeeSnapshots(YearMonth billingMonth) {
        if (billingMonth == null) {
            throw new BusinessException(BatchErrorCode.INVALID_BILLING_MONTH, "billingMonth must not be null");
        }

        String billingMonthText = billingMonth.toString();
        LocalDate startDate = billingMonth.atDay(1);
        LocalDate endDate = billingMonth.atEndOfMonth();
        List<MonthlyFeeSnapshot> snapshots;
        try {
            Map<String, List<String>> warehouseIdsBySeller = new LinkedHashMap<>();
            dailyBinSnapshotRepository.findBySnapshotDateBetween(startDate, endDate)
                    .forEach(snapshot -> warehouseIdsBySeller
                            .computeIfAbsent(snapshot.getSellerId(), ignored -> new ArrayList<>())
                            .add(snapshot.getWarehouseId()));
            wmsBillingReadRepository.findPickPackAggregations(billingMonth)
                    .forEach(aggregation -> warehouseIdsBySeller
                            .computeIfAbsent(aggregation.sellerId(), ignored -> new ArrayList<>())
                            .add(aggregation.warehouseId()));

            Map<String, MonthlyFeeSnapshot> snapshotByKey = new LinkedHashMap<>();
            wmsBillingReadRepository.findFeeSettings(billingMonth).forEach(summary -> {
                List<String> warehouseIds = resolveWarehouseIds(summary.sellerId(), summary.warehouseId(), warehouseIdsBySeller);
                for (String warehouseId : warehouseIds) {
                    MonthlyFeeSnapshot snapshot = MonthlyFeeSnapshot.of(
                            billingMonthText,
                            summary.sellerId(),
                            warehouseId,
                            summary.storageUnitPrice(),
                            summary.pickUnitPrice(),
                            summary.packUnitPrice()
                    );
                    snapshotByKey.put(summary.sellerId() + "::" + warehouseId, snapshot);
                }
            });

            snapshots = new ArrayList<>(snapshotByKey.values());
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

    private List<String> resolveWarehouseIds(
            String sellerId,
            String warehouseId,
            Map<String, List<String>> warehouseIdsBySeller
    ) {
        if (warehouseId != null && !warehouseId.isBlank()) {
            return List.of(warehouseId);
        }

        return warehouseIdsBySeller.getOrDefault(sellerId, List.of())
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }
}
