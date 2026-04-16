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
            WITH ranked_fee_settings AS (
                SELECT fs.tenant_id AS seller_id,
                       fs.warehouse_id AS warehouse_id,
                       fs.storage_pallet_rate_amt AS storage_unit_price,
                       fs.pick_base_rate_amt AS pick_unit_price,
                       fs.packing_material_rate_amt AS pack_unit_price,
                       ROW_NUMBER() OVER (
                           PARTITION BY fs.tenant_id, fs.warehouse_id
                           ORDER BY fs.effective_from DESC
                       ) AS row_num
                FROM fee_setting fs
                WHERE status = 'ACTIVE'
                  AND effective_from <= :billingMonthEndDate
            )
            SELECT seller_id,
                   warehouse_id,
                   storage_unit_price,
                   pick_unit_price,
                   pack_unit_price
            FROM ranked_fee_settings
            WHERE row_num = 1
            ORDER BY seller_id ASC, warehouse_id ASC
            """;

    private static final String FIND_PICK_PACK_AGGREGATIONS_SQL = """
            WITH picked AS (
                SELECT pp.tenant_id AS seller_id,
                       loc.warehouse_id AS warehouse_id,
                       COUNT(DISTINCT CONCAT(pp.order_id, '::', pp.sku_id, '::', pp.location_id)) AS pick_count
                FROM picking_packing pp
                INNER JOIN locations loc
                    ON loc.location_id = pp.location_id
                WHERE pp.started_at >= :billingMonthStartDateTime
                  AND pp.started_at < :nextBillingMonthStartDateTime
                GROUP BY pp.tenant_id, loc.warehouse_id
            ),
            packed AS (
                SELECT pp.tenant_id AS seller_id,
                       loc.warehouse_id AS warehouse_id,
                       COUNT(DISTINCT pp.order_id) AS pack_count
                FROM picking_packing pp
                INNER JOIN locations loc
                    ON loc.location_id = pp.location_id
                WHERE pp.completed_at >= :billingMonthStartDateTime
                  AND pp.completed_at < :nextBillingMonthStartDateTime
                GROUP BY pp.tenant_id, loc.warehouse_id
            ),
            aggregation_keys AS (
                SELECT seller_id, warehouse_id FROM picked
                UNION
                SELECT seller_id, warehouse_id FROM packed
            )
            SELECT aggregation_keys.seller_id AS seller_id,
                   aggregation_keys.warehouse_id AS warehouse_id,
                   COALESCE(picked.pick_count, 0) AS pick_count,
                   COALESCE(packed.pack_count, 0) AS pack_count
            FROM aggregation_keys
            LEFT JOIN picked
                ON picked.seller_id = aggregation_keys.seller_id
               AND picked.warehouse_id = aggregation_keys.warehouse_id
            LEFT JOIN packed
                ON packed.seller_id = aggregation_keys.seller_id
               AND packed.warehouse_id = aggregation_keys.warehouse_id
            ORDER BY aggregation_keys.seller_id ASC, aggregation_keys.warehouse_id ASC
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
