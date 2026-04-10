package com.conk.batch.billing.repository;

import com.conk.batch.billing.repository.dto.FeeSettingSummary;
import com.conk.batch.billing.repository.dto.PickPackMonthlyAggregation;
import java.time.YearMonth;
import java.util.List;

/**
 * WMS 읽기 전용 DB에서 정산 원천 데이터를 조회한다.
 */
public interface WmsBillingReadRepository {

    List<FeeSettingSummary> findFeeSettings(YearMonth billingMonth);

    List<PickPackMonthlyAggregation> findPickPackAggregations(YearMonth billingMonth);
}
