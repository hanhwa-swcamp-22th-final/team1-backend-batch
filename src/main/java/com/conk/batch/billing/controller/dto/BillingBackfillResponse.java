package com.conk.batch.billing.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillingBackfillResponse {

    private final String startDate;
    private final String endDate;
    private final int processedDays;
    private final int savedSnapshotCount;
}
