package com.conk.batch.billing.scheduler;

import com.conk.batch.billing.service.MonthlyBillingCalculationService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 전월 정산을 매월 1일에 계산한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.billing.scheduling", name = "enabled", havingValue = "true")
public class MonthlyBillingScheduler {

    @Value("${app.billing.scheduling.zone:Asia/Seoul}")
    private String zone;

    private final MonthlyBillingCalculationService monthlyBillingCalculationService;

    @Scheduled(
            cron = "${app.billing.scheduling.monthly-billing-cron}",
            zone = "${app.billing.scheduling.zone:Asia/Seoul}"
    )
    public void run() {
        YearMonth previousMonth = YearMonth.from(LocalDate.now(ZoneId.of(zone)).minusMonths(1));
        monthlyBillingCalculationService.calculateAndPublish(previousMonth);
    }
}
