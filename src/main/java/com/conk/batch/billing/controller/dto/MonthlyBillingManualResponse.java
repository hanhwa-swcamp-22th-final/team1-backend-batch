package com.conk.batch.billing.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyBillingManualResponse {

    private final String billingMonth;
    private final int processedDays;
    private final int savedSnapshotCount;
    private final int calculatedBillingCount;
}
