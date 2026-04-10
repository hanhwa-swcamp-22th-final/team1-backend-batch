package com.conk.batch.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conk.batch.billing.domain.BillingDispatchHistory;
import com.conk.batch.billing.domain.BillingDispatchStatus;
import com.conk.batch.billing.domain.BillingStatus;
import com.conk.batch.billing.domain.DailyBinSnapshot;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MonthlyBillingCalculationServiceTest {

    @Mock
    private DailyBinSnapshotRepository dailyBinSnapshotRepository;

    @Mock
    private SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    @Mock
    private BillingDispatchHistoryRepository billingDispatchHistoryRepository;

    @Mock
    private WmsBillingReadRepository wmsBillingReadRepository;

    @Mock
    private MonthlyFeeSnapshotService monthlyFeeSnapshotService;

    @Mock
    private StorageFeeCalculator storageFeeCalculator;

    @Mock
    private PickPackFeeCalculator pickPackFeeCalculator;

    @Mock
    private BillingResultEventPublisher billingResultEventPublisher;

    @InjectMocks
    private MonthlyBillingCalculationService monthlyBillingCalculationService;

    @Test
    @DisplayName("월 정산 계산 성공: 전월 snapshot과 집계 결과를 저장하고 발행 성공 상태로 마무리한다")
    @SuppressWarnings("unchecked")
    void calculateAndPublish_success() {
        ReflectionTestUtils.setField(
                monthlyBillingCalculationService,
                "billingMonthlyResultTopic",
                "billing.monthly.result.v1"
        );
        YearMonth billingMonth = YearMonth.of(2026, 3);
        MonthlyFeeSnapshot feeSnapshot = MonthlyFeeSnapshot.of(
                "2026-03",
                "SELLER-001",
                "WH-001",
                new BigDecimal("28500"),
                new BigDecimal("2500"),
                new BigDecimal("2500")
        );
        when(monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(billingMonth)).thenReturn(List.of(feeSnapshot));
        when(dailyBinSnapshotRepository.findBySnapshotDateBetween(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        )).thenReturn(List.of(
                DailyBinSnapshot.of(LocalDate.of(2026, 3, 1), "SELLER-001", "WH-001", 31),
                DailyBinSnapshot.of(LocalDate.of(2026, 3, 2), "SELLER-001", "WH-001", 31)
        ));
        when(wmsBillingReadRepository.findPickPackAggregations(billingMonth)).thenReturn(List.of(
                new PickPackMonthlyAggregation("SELLER-001", "WH-001", 3, 1)
        ));
        when(storageFeeCalculator.calculate(62, new BigDecimal("28500"), billingMonth))
                .thenReturn(new StorageFeeCalculator.StorageFeeResult(new BigDecimal("2.00"), new BigDecimal("57000")));
        when(pickPackFeeCalculator.calculate(3, new BigDecimal("2500"))).thenReturn(new BigDecimal("7500"));
        when(pickPackFeeCalculator.calculate(1, new BigDecimal("2500"))).thenReturn(new BigDecimal("2500"));
        when(sellerMonthlyBillingRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billingDispatchHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SellerMonthlyBilling> result = monthlyBillingCalculationService.calculateAndPublish(billingMonth);

        ArgumentCaptor<List<SellerMonthlyBilling>> billingCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<BillingDispatchHistory> historyCaptor = ArgumentCaptor.forClass(BillingDispatchHistory.class);
        verify(sellerMonthlyBillingRepository).deleteByBillingMonth("2026-03");
        verify(sellerMonthlyBillingRepository, times(2)).flush();
        verify(sellerMonthlyBillingRepository).saveAllAndFlush(billingCaptor.capture());
        verify(billingDispatchHistoryRepository).save(historyCaptor.capture());
        verify(billingResultEventPublisher).publish(eq(result.get(0)), eq(feeSnapshot));

        SellerMonthlyBilling saved = billingCaptor.getValue().get(0);
        BillingDispatchHistory history = historyCaptor.getValue();

        assertEquals(1, result.size());
        assertEquals(62, saved.getOccupiedBinDays());
        assertEquals(new BigDecimal("2.00"), saved.getAverageOccupiedBins());
        assertEquals(new BigDecimal("57000"), saved.getStorageFee());
        assertEquals(3, saved.getPickCount());
        assertEquals(new BigDecimal("7500"), saved.getPickingFee());
        assertEquals(1, saved.getPackCount());
        assertEquals(new BigDecimal("2500"), saved.getPackingFee());
        assertEquals(new BigDecimal("67000"), saved.getTotalFee());
        assertEquals(BillingStatus.DISPATCHED, saved.getStatus());

        assertEquals("2026-03", history.getBillingMonth());
        assertEquals("SELLER-001", history.getSellerId());
        assertEquals("billing.monthly.result.v1", history.getTopicName());
        assertEquals(BillingDispatchStatus.SUCCESS, history.getDispatchStatus());
        assertEquals(null, history.getErrorMessage());
    }

    @Test
    @DisplayName("월 정산 계산 실패: 기준월이 없으면 어떤 조회와 저장도 수행하지 않고 예외를 전파한다")
    void calculateAndPublish_whenBillingMonthIsNull_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> monthlyBillingCalculationService.calculateAndPublish(null));

        assertEquals(BatchErrorCode.INVALID_BILLING_MONTH, exception.getErrorCode());
        assertEquals("billingMonth must not be null", exception.getMessage());
        verify(monthlyFeeSnapshotService, never()).captureMonthlyFeeSnapshots(any());
        verify(wmsBillingReadRepository, never()).findPickPackAggregations(any());
        verify(sellerMonthlyBillingRepository, never()).deleteByBillingMonth(any());
        verify(sellerMonthlyBillingRepository, never()).saveAllAndFlush(any());
        verify(billingResultEventPublisher, never()).publish(any(), any());
    }

    @Test
    @DisplayName("월 정산 계산 실패: pick-pack 집계 조회 예외가 발생하면 정산 결과를 저장하지 않고 예외를 래핑한다")
    void calculateAndPublish_whenPickPackAggregationThrows_thenWrapException() {
        YearMonth billingMonth = YearMonth.of(2026, 3);
        when(monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(billingMonth)).thenReturn(List.of(
                MonthlyFeeSnapshot.of(
                        "2026-03",
                        "SELLER-001",
                        "WH-001",
                        new BigDecimal("28500"),
                        new BigDecimal("2500"),
                        new BigDecimal("2500")
                )
        ));
        when(dailyBinSnapshotRepository.findBySnapshotDateBetween(any(), any())).thenReturn(List.of());
        when(wmsBillingReadRepository.findPickPackAggregations(billingMonth))
                .thenThrow(new IllegalStateException("WMS read unavailable"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> monthlyBillingCalculationService.calculateAndPublish(billingMonth));

        assertEquals(BatchErrorCode.PICK_PACK_AGGREGATION_FETCH_FAILED, exception.getErrorCode());
        assertEquals("failed to fetch pick-pack aggregations from WMS for billingMonth=2026-03", exception.getMessage());
        verify(sellerMonthlyBillingRepository, never()).deleteByBillingMonth(any());
        verify(sellerMonthlyBillingRepository, never()).saveAllAndFlush(any());
        verify(billingDispatchHistoryRepository, never()).save(any());
        verify(billingResultEventPublisher, never()).publish(any(), any());
    }

    @Test
    @DisplayName("월 정산 발행 실패: Kafka 발행 예외가 발생해도 정산 결과와 실패 이력을 남긴다")
    @SuppressWarnings("unchecked")
    void calculateAndPublish_whenPublisherThrows_thenMarkFailed() {
        ReflectionTestUtils.setField(
                monthlyBillingCalculationService,
                "billingMonthlyResultTopic",
                "billing.monthly.result.v1"
        );
        YearMonth billingMonth = YearMonth.of(2026, 3);
        MonthlyFeeSnapshot feeSnapshot = MonthlyFeeSnapshot.of(
                "2026-03",
                "SELLER-001",
                "WH-001",
                new BigDecimal("28500"),
                new BigDecimal("2500"),
                new BigDecimal("2500")
        );
        when(monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(billingMonth)).thenReturn(List.of(feeSnapshot));
        when(dailyBinSnapshotRepository.findBySnapshotDateBetween(any(), any())).thenReturn(List.of());
        when(wmsBillingReadRepository.findPickPackAggregations(billingMonth)).thenReturn(List.of());
        when(storageFeeCalculator.calculate(0, new BigDecimal("28500"), billingMonth))
                .thenReturn(new StorageFeeCalculator.StorageFeeResult(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(0)));
        when(pickPackFeeCalculator.calculate(0, new BigDecimal("2500"))).thenReturn(BigDecimal.ZERO.setScale(0));
        when(sellerMonthlyBillingRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billingDispatchHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new BusinessException(BatchErrorCode.BILLING_RESULT_PUBLISH_FAILED, "kafka failed"))
                .when(billingResultEventPublisher)
                .publish(any(), any());

        List<SellerMonthlyBilling> result = monthlyBillingCalculationService.calculateAndPublish(billingMonth);

        ArgumentCaptor<BillingDispatchHistory> historyCaptor = ArgumentCaptor.forClass(BillingDispatchHistory.class);
        verify(billingDispatchHistoryRepository).save(historyCaptor.capture());

        assertEquals(1, result.size());
        assertEquals(BillingStatus.PUBLISH_FAILED, result.get(0).getStatus());
        assertEquals(BillingDispatchStatus.FAILED, historyCaptor.getValue().getDispatchStatus());
        assertEquals("kafka failed", historyCaptor.getValue().getErrorMessage());
    }
}
