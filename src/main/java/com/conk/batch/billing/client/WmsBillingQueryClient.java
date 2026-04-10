package com.conk.batch.billing.client;

import com.conk.batch.billing.client.dto.BinCountSummaryResponse;
import com.conk.batch.common.config.FeignConfig;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * WMS 내부 정산 조회 API 클라이언트다.
 */
@FeignClient(
        name = "wmsBillingQueryClient",
        url = "${app.clients.wms.base-url}",
        configuration = FeignConfig.class
)
public interface WmsBillingQueryClient {

    @GetMapping("/wms/internal/billing/bin-counts")
    List<BinCountSummaryResponse> getBinCountSummaries(
            @RequestParam("baseDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate baseDate
    );
}
