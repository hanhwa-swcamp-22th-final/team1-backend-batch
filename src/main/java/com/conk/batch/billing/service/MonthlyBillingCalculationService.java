package com.conk.batch.billing.service;

import com.conk.batch.billing.domain.BillingDispatchHistory;
import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.domain.SellerMonthlyBilling;
import com.conk.batch.billing.publisher.BillingResultEventPublisher;
import com.conk.batch.billing.repository.BillingDispatchHistoryRepository;
import com.conk.batch.billing.repository.DailyBinSnapshotRepository;
import com.conk.batch.billing.repository.SellerMonthlyBillingRepository;
import com.conk.batch.billing.repository.WmsBillingReadRepository;
import com.conk.batch.billing.repository.dto.PickPackMonthlyAggregation;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 월 정산 계산과 결과 발행을 오케스트레이션한다.
 */
@Service
@RequiredArgsConstructor
public class MonthlyBillingCalculationService {

    @Value("${app.kafka.topics.billing-monthly-result:billing.monthly.result.v1}")
    private String billingMonthlyResultTopic;

    private final DailyBinSnapshotRepository dailyBinSnapshotRepository;
    private final SellerMonthlyBillingRepository sellerMonthlyBillingRepository;
    private final BillingDispatchHistoryRepository billingDispatchHistoryRepository;
    private final WmsBillingReadRepository wmsBillingReadRepository;
    private final MonthlyFeeSnapshotService monthlyFeeSnapshotService;
    private final StorageFeeCalculator storageFeeCalculator;
    private final PickPackFeeCalculator pickPackFeeCalculator;
    private final BillingResultEventPublisher billingResultEventPublisher;

    @Transactional
    public List<SellerMonthlyBilling> calculateAndPublish(YearMonth billingMonth) {
        if (billingMonth == null) {
            throw new BusinessException(BatchErrorCode.INVALID_BILLING_MONTH, "billingMonth must not be null");
        }

        String billingMonthText = billingMonth.toString();
        LocalDate startDate = billingMonth.atDay(1);
        LocalDate endDate = billingMonth.atEndOfMonth();

        List<MonthlyFeeSnapshot> feeSnapshots = monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(billingMonth);
        Map<SellerWarehouseKey, MonthlyFeeSnapshot> feeSnapshotByKey = feeSnapshots
                .stream()
                .collect(Collectors.toMap(
                        snapshot -> new SellerWarehouseKey(snapshot.getSellerId(), snapshot.getWarehouseId()),
                        Function.identity(),
                        (left, right) -> right
                ));

        Map<SellerWarehouseKey, Integer> occupiedBinDaysByKey = dailyBinSnapshotRepository
                .findBySnapshotDateBetween(startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(
                        snapshot -> new SellerWarehouseKey(snapshot.getSellerId(), snapshot.getWarehouseId()),
                        Collectors.summingInt(snapshot -> snapshot.getOccupiedBinCount() == null ? 0 : snapshot.getOccupiedBinCount())
                ));

        Map<SellerWarehouseKey, PickPackMonthlyAggregation> pickPackByKey = loadPickPackAggregations(billingMonth);

        sellerMonthlyBillingRepository.deleteByBillingMonth(billingMonthText);
        sellerMonthlyBillingRepository.flush();

        List<SellerMonthlyBilling> savedBillings = sellerMonthlyBillingRepository.saveAllAndFlush(
                feeSnapshots.stream()
                        .map(feeSnapshot -> createBilling(
                                billingMonth,
                                feeSnapshot,
                                occupiedBinDaysByKey,
                                pickPackByKey
                        ))
                        .toList()
        );

        savedBillings.forEach(billing -> publish(billing, feeSnapshotByKey.get(keyOf(billing))));
        sellerMonthlyBillingRepository.flush();
        billingDispatchHistoryRepository.flush();
        return savedBillings;
    }

    private SellerMonthlyBilling createBilling(
            YearMonth billingMonth,
            MonthlyFeeSnapshot feeSnapshot,
            Map<SellerWarehouseKey, Integer> occupiedBinDaysByKey,
            Map<SellerWarehouseKey, PickPackMonthlyAggregation> pickPackByKey
    ) {
        SellerWarehouseKey key = new SellerWarehouseKey(feeSnapshot.getSellerId(), feeSnapshot.getWarehouseId());
        int occupiedBinDays = occupiedBinDaysByKey.getOrDefault(key, 0);
        PickPackMonthlyAggregation aggregation = pickPackByKey.getOrDefault(
                key,
                PickPackMonthlyAggregation.empty(feeSnapshot.getSellerId(), feeSnapshot.getWarehouseId())
        );

        StorageFeeCalculator.StorageFeeResult storageFeeResult = storageFeeCalculator.calculate(
                occupiedBinDays,
                feeSnapshot.getStorageUnitPrice(),
                billingMonth
        );

        return SellerMonthlyBilling.calculated(
                billingMonth.toString(),
                feeSnapshot.getSellerId(),
                feeSnapshot.getWarehouseId(),
                occupiedBinDays,
                storageFeeResult.averageOccupiedBins(),
                storageFeeResult.storageFee(),
                aggregation.pickCount(),
                pickPackFeeCalculator.calculate(aggregation.pickCount(), feeSnapshot.getPickUnitPrice()),
                aggregation.packCount(),
                pickPackFeeCalculator.calculate(aggregation.packCount(), feeSnapshot.getPackUnitPrice())
        );
    }

    private void publish(SellerMonthlyBilling billing, MonthlyFeeSnapshot feeSnapshot) {
        BillingDispatchHistory history = billingDispatchHistoryRepository.save(BillingDispatchHistory.pending(
                billing.getBillingMonth(),
                billing.getSellerId(),
                billingMonthlyResultTopic
        ));

        try {
            billingResultEventPublisher.publish(billing, feeSnapshot);
            history.markSuccess();
            billing.markDispatched();
        } catch (Exception exception) {
            history.markFailed(exception.getMessage());
            billing.markPublishFailed();
        }
    }

    private Map<SellerWarehouseKey, PickPackMonthlyAggregation> loadPickPackAggregations(YearMonth billingMonth) {
        try {
            return wmsBillingReadRepository.findPickPackAggregations(billingMonth)
                    .stream()
                    .collect(Collectors.toMap(
                            aggregation -> new SellerWarehouseKey(aggregation.sellerId(), aggregation.warehouseId()),
                            Function.identity(),
                            (left, right) -> new PickPackMonthlyAggregation(
                                    right.sellerId(),
                                    right.warehouseId(),
                                    left.pickCount() + right.pickCount(),
                                    left.packCount() + right.packCount()
                            )
                    ));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    BatchErrorCode.PICK_PACK_AGGREGATION_FETCH_FAILED,
                    "failed to fetch pick-pack aggregations from WMS for billingMonth=" + billingMonth,
                    exception
            );
        }
    }

    private SellerWarehouseKey keyOf(SellerMonthlyBilling billing) {
        return new SellerWarehouseKey(billing.getSellerId(), billing.getWarehouseId());
    }

    private record SellerWarehouseKey(String sellerId, String warehouseId) {
    }
}
