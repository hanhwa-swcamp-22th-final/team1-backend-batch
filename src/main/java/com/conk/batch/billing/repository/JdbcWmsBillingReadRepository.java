package com.conk.batch.billing.repository;

import com.conk.batch.billing.repository.dto.FeeSettingSummary;
import com.conk.batch.billing.repository.dto.PickPackMonthlyAggregation;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * WMS read datasource 기반 정산 조회 구현체다.
 *
 * 1단계에서는 SQL 시그니처와 의존성만 고정하고,
 * 3단계에서 fee_setting / picking_packing 집계 SQL을 채운다.
 */
@Repository
@RequiredArgsConstructor
public class JdbcWmsBillingReadRepository implements WmsBillingReadRepository {

    @SuppressWarnings("unused")
    private final NamedParameterJdbcTemplate wmsReadNamedParameterJdbcTemplate;

    @Override
    public List<FeeSettingSummary> findFeeSettings(YearMonth billingMonth) {
        return Collections.emptyList();
    }

    @Override
    public List<PickPackMonthlyAggregation> findPickPackAggregations(YearMonth billingMonth) {
        return Collections.emptyList();
    }
}
