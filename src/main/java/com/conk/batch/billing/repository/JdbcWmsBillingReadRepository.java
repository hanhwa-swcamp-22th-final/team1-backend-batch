package com.conk.batch.billing.repository;

import com.conk.batch.billing.repository.dto.FeeSettingSummary;
import com.conk.batch.billing.repository.dto.PickPackMonthlyAggregation;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * WMS read datasource 기반 정산 조회 구현체다.
 */
@Repository
@RequiredArgsConstructor
public class JdbcWmsBillingReadRepository implements WmsBillingReadRepository {

    private final NamedParameterJdbcTemplate wmsReadNamedParameterJdbcTemplate;

    private static final RowMapper<FeeSettingSummary> FEE_SETTING_ROW_MAPPER = (resultSet, rowNum) ->
            new FeeSettingSummary(
                    resultSet.getString("seller_id"),
                    resultSet.getString("warehouse_id"),
                    resultSet.getBigDecimal("storage_unit_price"),
                    resultSet.getBigDecimal("pick_unit_price"),
                    resultSet.getBigDecimal("pack_unit_price")
            );

    private static final RowMapper<PickPackMonthlyAggregation> PICK_PACK_ROW_MAPPER = (resultSet, rowNum) ->
            new PickPackMonthlyAggregation(
                    resultSet.getString("seller_id"),
                    resultSet.getString("warehouse_id"),
                    resultSet.getInt("pick_count"),
                    resultSet.getInt("pack_count")
            );

    private static final String FIND_FEE_SETTINGS_SQL = """
            SELECT fs.tenant_id AS seller_id,
                   fs.warehouse_id AS warehouse_id,
                   fs.storage_pallet_rate_amt AS storage_unit_price,
                   fs.pick_base_rate_amt AS pick_unit_price,
                   fs.packing_material_rate_amt AS pack_unit_price
            FROM fee_setting fs
            INNER JOIN (
                SELECT tenant_id, warehouse_id, MAX(effective_from) AS latest_effective_from
                FROM fee_setting
                WHERE status = 'ACTIVE'
                  AND effective_from <= :billingMonthEndDate
                GROUP BY tenant_id, warehouse_id
            ) latest
                ON latest.tenant_id = fs.tenant_id
               AND latest.warehouse_id <=> fs.warehouse_id
               AND latest.latest_effective_from = fs.effective_from
            WHERE fs.status = 'ACTIVE'
            ORDER BY fs.tenant_id ASC, fs.warehouse_id ASC
            """;

    private static final String FIND_PICK_PACK_AGGREGATIONS_SQL = """
            SELECT aggregated.seller_id AS seller_id,
                   aggregated.warehouse_id AS warehouse_id,
                   SUM(aggregated.pick_count) AS pick_count,
                   SUM(aggregated.pack_count) AS pack_count
            FROM (
                SELECT pp.tenant_id AS seller_id,
                       loc.warehouse_id AS warehouse_id,
                       COUNT(DISTINCT CONCAT(pp.order_id, '::', pp.sku_id, '::', pp.location_id)) AS pick_count,
                       0 AS pack_count
                FROM picking_packing pp
                INNER JOIN locations loc
                    ON loc.location_id = pp.location_id
                WHERE pp.started_at >= :billingMonthStartDateTime
                  AND pp.started_at < :nextBillingMonthStartDateTime
                GROUP BY pp.tenant_id, loc.warehouse_id

                UNION ALL

                SELECT pp.tenant_id AS seller_id,
                       loc.warehouse_id AS warehouse_id,
                       0 AS pick_count,
                       COUNT(DISTINCT pp.order_id) AS pack_count
                FROM picking_packing pp
                INNER JOIN locations loc
                    ON loc.location_id = pp.location_id
                WHERE pp.completed_at >= :billingMonthStartDateTime
                  AND pp.completed_at < :nextBillingMonthStartDateTime
                GROUP BY pp.tenant_id, loc.warehouse_id
            ) aggregated
            GROUP BY aggregated.seller_id, aggregated.warehouse_id
            ORDER BY aggregated.seller_id ASC, aggregated.warehouse_id ASC
            """;

    @Override
    public List<FeeSettingSummary> findFeeSettings(YearMonth billingMonth) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("billingMonthEndDate", billingMonth.atEndOfMonth());
        return wmsReadNamedParameterJdbcTemplate.query(
                FIND_FEE_SETTINGS_SQL,
                parameters,
                FEE_SETTING_ROW_MAPPER
        );
    }

    @Override
    public List<PickPackMonthlyAggregation> findPickPackAggregations(YearMonth billingMonth) {
        LocalDateTime billingMonthStartDateTime = billingMonth.atDay(1).atStartOfDay();
        LocalDateTime nextBillingMonthStartDateTime = billingMonth.plusMonths(1).atDay(1).atStartOfDay();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("billingMonthStartDateTime", billingMonthStartDateTime)
                .addValue("nextBillingMonthStartDateTime", nextBillingMonthStartDateTime);
        return wmsReadNamedParameterJdbcTemplate.query(
                FIND_PICK_PACK_AGGREGATIONS_SQL,
                parameters,
                PICK_PACK_ROW_MAPPER
        );
    }
}
