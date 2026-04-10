package com.conk.batch.billing.scheduler;

import com.conk.batch.billing.service.DailyBinSnapshotService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 occupied bin 스냅샷을 적재한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.billing.scheduling", name = "enabled", havingValue = "true")
public class DailyBinSnapshotScheduler {

    @Value("${app.billing.scheduling.zone:Asia/Seoul}")
    private String zone;

    private final DailyBinSnapshotService dailyBinSnapshotService;

    @Scheduled(
            cron = "${app.billing.scheduling.daily-bin-snapshot-cron}",
            zone = "${app.billing.scheduling.zone:Asia/Seoul}"
    )
    public void run() {
        dailyBinSnapshotService.captureDailySnapshots(LocalDate.now(ZoneId.of(zone)));
    }
}
