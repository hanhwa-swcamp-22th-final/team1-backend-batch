package com.conk.batch.billing.service;

import com.conk.batch.billing.controller.dto.BillingBackfillResponse;
import com.conk.batch.billing.controller.dto.MonthlyBillingManualResponse;
import com.conk.batch.billing.domain.SellerMonthlyBilling;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BillingManualExecutionService {

    private final DailyBinSnapshotService dailyBinSnapshotService;
    private final MonthlyBillingCalculationService monthlyBillingCalculationService;

    public BillingBackfillResponse backfillDailySnapshots(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        int processedDays = 0;
        int savedSnapshotCount = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            savedSnapshotCount += dailyBinSnapshotService.captureDailySnapshots(currentDate).size();
            processedDays++;
            currentDate = currentDate.plusDays(1);
        }

        return BillingBackfillResponse.builder()
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .processedDays(processedDays)
                .savedSnapshotCount(savedSnapshotCount)
                .build();
    }

    public MonthlyBillingManualResponse calculateMonthlyBilling(String billingMonthText) {
        YearMonth billingMonth = parseBillingMonth(billingMonthText);
        return buildMonthlyResponse(billingMonth, 0, 0);
    }

    public MonthlyBillingManualResponse backfillAndPublish(LocalDate startDate, LocalDate endDate, String billingMonthText) {
        BillingBackfillResponse backfillResponse = backfillDailySnapshots(startDate, endDate);
        YearMonth billingMonth = parseBillingMonth(billingMonthText);
        return buildMonthlyResponse(
                billingMonth,
                backfillResponse.getProcessedDays(),
                backfillResponse.getSavedSnapshotCount()
        );
    }

    private MonthlyBillingManualResponse buildMonthlyResponse(YearMonth billingMonth, int processedDays, int savedSnapshotCount) {
        java.util.List<SellerMonthlyBilling> billings = monthlyBillingCalculationService.calculateAndPublish(billingMonth);
        return MonthlyBillingManualResponse.builder()
                .billingMonth(billingMonth.toString())
                .processedDays(processedDays)
                .savedSnapshotCount(savedSnapshotCount)
                .calculatedBillingCount(billings.size())
                .build();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BusinessException(BatchErrorCode.INVALID_SNAPSHOT_DATE, "startDate/endDate range is invalid");
        }
    }

    private YearMonth parseBillingMonth(String billingMonthText) {
        try {
            return YearMonth.parse(billingMonthText);
        } catch (Exception exception) {
            throw new BusinessException(BatchErrorCode.INVALID_BILLING_MONTH, "billingMonth must follow yyyy-MM format", exception);
        }
    }
}
