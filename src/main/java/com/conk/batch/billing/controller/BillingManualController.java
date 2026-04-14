package com.conk.batch.billing.controller;

import com.conk.batch.billing.controller.dto.BillingBackfillResponse;
import com.conk.batch.billing.controller.dto.MonthlyBillingManualResponse;
import com.conk.batch.billing.service.BillingManualExecutionService;
import com.conk.batch.common.dto.ApiResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batch/billing/manual")
public class BillingManualController {

    private final BillingManualExecutionService billingManualExecutionService;

    public BillingManualController(BillingManualExecutionService billingManualExecutionService) {
        this.billingManualExecutionService = billingManualExecutionService;
    }

    @PostMapping("/daily-snapshots")
    public ResponseEntity<ApiResponse<BillingBackfillResponse>> backfillDailySnapshots(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "일별 occupied bin 스냅샷 백필을 실행했습니다.",
                billingManualExecutionService.backfillDailySnapshots(startDate, endDate)
        ));
    }

    @PostMapping("/monthly-results")
    public ResponseEntity<ApiResponse<MonthlyBillingManualResponse>> calculateMonthlyBilling(
            @RequestParam String billingMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "월 정산 계산 및 발행을 실행했습니다.",
                billingManualExecutionService.calculateMonthlyBilling(billingMonth)
        ));
    }

    @PostMapping("/backfill-and-publish")
    public ResponseEntity<ApiResponse<MonthlyBillingManualResponse>> backfillAndPublish(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam String billingMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "일별 스냅샷 백필 후 월 정산 계산 및 발행을 실행했습니다.",
                billingManualExecutionService.backfillAndPublish(startDate, endDate, billingMonth)
        ));
    }
}
