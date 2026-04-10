package com.conk.batch.billing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.conk.batch.billing.repository.dto.FeeSettingSummary;
import com.conk.batch.billing.repository.dto.PickPackMonthlyAggregation;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@SpringBootTest
class JdbcWmsBillingReadRepositoryTest {

    @Autowired
    private JdbcWmsBillingReadRepository jdbcWmsBillingReadRepository;

    @Autowired
    @Qualifier("wmsReadDataSourceProperties")
    private DataSourceProperties wmsReadDataSourceProperties;

    private JdbcTemplate writableWmsJdbcTemplate;

    @BeforeEach
    void setUp() {
        writableWmsJdbcTemplate = new JdbcTemplate(createWritableWmsDataSource());
        resetWmsReadSchema();
    }

    @Test
    @DisplayName("기준월 fee_setting 조회 성공: 월말 기준 최신 ACTIVE 요금 설정을 seller와 warehouse별로 조회한다")
    void findFeeSettings_success() {
        insertFeeSetting("SELLER-001", "WH-001", "ACTIVE", LocalDate.of(2026, 1, 1), "28000", "2400", "2300");
        insertFeeSetting("SELLER-001", "WH-001", "ACTIVE", LocalDate.of(2026, 3, 15), "28500", "2500", "2500");
        insertFeeSetting("SELLER-001", "WH-001", "ACTIVE", LocalDate.of(2026, 4, 1), "30000", "2600", "2600");
        insertFeeSetting("SELLER-001", "WH-002", "ACTIVE", LocalDate.of(2026, 2, 1), "31000", "2600", "2400");
        insertFeeSetting("SELLER-002", "WH-001", "INACTIVE", LocalDate.of(2026, 3, 1), "99999", "9999", "9999");
        insertFeeSetting("SELLER-002", "WH-001", "ACTIVE", LocalDate.of(2026, 2, 20), "27000", "2300", "2100");

        List<FeeSettingSummary> result = jdbcWmsBillingReadRepository.findFeeSettings(YearMonth.of(2026, 3));

        assertThat(result)
                .extracting(
                        FeeSettingSummary::sellerId,
                        FeeSettingSummary::warehouseId,
                        FeeSettingSummary::storageUnitPrice,
                        FeeSettingSummary::pickUnitPrice,
                        FeeSettingSummary::packUnitPrice
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SELLER-001", "WH-001", new BigDecimal("28500.00"), new BigDecimal("2500.00"), new BigDecimal("2500.00")),
                        org.assertj.core.groups.Tuple.tuple("SELLER-001", "WH-002", new BigDecimal("31000.00"), new BigDecimal("2600.00"), new BigDecimal("2400.00")),
                        org.assertj.core.groups.Tuple.tuple("SELLER-002", "WH-001", new BigDecimal("27000.00"), new BigDecimal("2300.00"), new BigDecimal("2100.00"))
                );
    }

    @Test
    @DisplayName("기준월 fee_setting 조회 시 조건에 맞는 ACTIVE 요금 설정이 없으면 빈 목록을 반환한다")
    void findFeeSettings_whenNoApplicableRows_thenReturnEmpty() {
        insertFeeSetting("SELLER-001", "WH-001", "INACTIVE", LocalDate.of(2026, 3, 1), "28000", "2400", "2300");
        insertFeeSetting("SELLER-001", "WH-001", "ACTIVE", LocalDate.of(2026, 4, 1), "30000", "2600", "2600");

        List<FeeSettingSummary> result = jdbcWmsBillingReadRepository.findFeeSettings(YearMonth.of(2026, 3));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("기준월 picking_packing 집계 성공: started_at과 completed_at 기준으로 pick과 pack를 seller와 warehouse별로 집계한다")
    void findPickPackAggregations_success() {
        insertLocation("LOC-A-01-01", "BIN-A-01-01", "WH-001");
        insertLocation("LOC-A-01-02", "BIN-A-01-02", "WH-001");
        insertLocation("LOC-B-01-01", "BIN-B-01-01", "WH-002");

        insertPickingPacking("SELLER-001", "ORD-001", "SKU-001", "LOC-A-01-01",
                LocalDateTime.of(2026, 3, 5, 9, 0), LocalDateTime.of(2026, 3, 6, 18, 0));
        insertPickingPacking("SELLER-001", "ORD-001", "SKU-002", "LOC-A-01-02",
                LocalDateTime.of(2026, 3, 5, 9, 5), LocalDateTime.of(2026, 3, 6, 18, 5));
        insertPickingPacking("SELLER-001", "ORD-002", "SKU-003", "LOC-A-01-01",
                LocalDateTime.of(2026, 3, 10, 10, 0), null);
        insertPickingPacking("SELLER-001", "ORD-003", "SKU-004", "LOC-B-01-01",
                LocalDateTime.of(2026, 3, 12, 11, 0), LocalDateTime.of(2026, 4, 1, 9, 0));
        insertPickingPacking("SELLER-001", "ORD-004", "SKU-005", "LOC-A-01-02",
                LocalDateTime.of(2026, 2, 28, 23, 0), LocalDateTime.of(2026, 3, 1, 9, 0));
        insertPickingPacking("SELLER-002", "ORD-010", "SKU-001", "LOC-A-01-01",
                LocalDateTime.of(2026, 3, 7, 8, 0), LocalDateTime.of(2026, 3, 8, 9, 0));

        List<PickPackMonthlyAggregation> result = jdbcWmsBillingReadRepository.findPickPackAggregations(YearMonth.of(2026, 3));

        assertThat(result)
                .extracting(
                        PickPackMonthlyAggregation::sellerId,
                        PickPackMonthlyAggregation::warehouseId,
                        PickPackMonthlyAggregation::pickCount,
                        PickPackMonthlyAggregation::packCount
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SELLER-001", "WH-001", 3, 2),
                        org.assertj.core.groups.Tuple.tuple("SELLER-001", "WH-002", 1, 0),
                        org.assertj.core.groups.Tuple.tuple("SELLER-002", "WH-001", 1, 1)
                );
    }

    @Test
    @DisplayName("기준월 picking_packing 집계 시 조건에 맞는 row가 없으면 빈 목록을 반환한다")
    void findPickPackAggregations_whenNoApplicableRows_thenReturnEmpty() {
        insertLocation("LOC-A-01-01", "BIN-A-01-01", "WH-001");
        insertPickingPacking("SELLER-001", "ORD-001", "SKU-001", "LOC-A-01-01",
                LocalDateTime.of(2026, 2, 28, 9, 0), LocalDateTime.of(2026, 4, 1, 9, 0));

        List<PickPackMonthlyAggregation> result = jdbcWmsBillingReadRepository.findPickPackAggregations(YearMonth.of(2026, 3));

        assertThat(result).isEmpty();
    }

    private DataSource createWritableWmsDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(wmsReadDataSourceProperties.getDriverClassName());
        dataSource.setUrl(wmsReadDataSourceProperties.getUrl());
        dataSource.setUsername(wmsReadDataSourceProperties.getUsername());
        dataSource.setPassword(wmsReadDataSourceProperties.getPassword());
        return dataSource;
    }

    private void resetWmsReadSchema() {
        writableWmsJdbcTemplate.execute("DROP TABLE IF EXISTS picking_packing");
        writableWmsJdbcTemplate.execute("DROP TABLE IF EXISTS locations");
        writableWmsJdbcTemplate.execute("DROP TABLE IF EXISTS fee_setting");

        writableWmsJdbcTemplate.execute("""
                CREATE TABLE fee_setting (
                    fee_setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id VARCHAR(255) NOT NULL,
                    warehouse_id VARCHAR(255),
                    status VARCHAR(255) NOT NULL,
                    effective_from DATE NOT NULL,
                    storage_pallet_rate_amt NUMERIC(38, 2) NOT NULL,
                    storage_min_billing_unit INTEGER NOT NULL,
                    storage_prorata_rule VARCHAR(255) NOT NULL,
                    pick_base_rate_amt NUMERIC(38, 2) NOT NULL,
                    pick_additional_sku_rate_amt NUMERIC(38, 2) NOT NULL,
                    packing_material_rate_amt NUMERIC(38, 2) NOT NULL,
                    special_packaging_surcharge_amt NUMERIC(38, 2) NOT NULL,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    created_by VARCHAR(255),
                    updated_by VARCHAR(255)
                )
                """);
        writableWmsJdbcTemplate.execute("""
                CREATE TABLE locations (
                    location_id VARCHAR(255) PRIMARY KEY,
                    bin_id VARCHAR(255) NOT NULL UNIQUE,
                    warehouse_id VARCHAR(255) NOT NULL,
                    zone_id VARCHAR(255) NOT NULL,
                    rack_id VARCHAR(255) NOT NULL,
                    worker_account_id VARCHAR(255),
                    capacity_quantity INTEGER NOT NULL,
                    is_active BOOLEAN NOT NULL
                )
                """);
        writableWmsJdbcTemplate.execute("""
                CREATE TABLE picking_packing (
                    sku_id VARCHAR(255) NOT NULL,
                    location_id VARCHAR(255) NOT NULL,
                    tenant_id VARCHAR(255) NOT NULL,
                    order_id VARCHAR(255) NOT NULL,
                    picked_quantity INTEGER,
                    packed_quantity INTEGER,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    created_by VARCHAR(255),
                    updated_by VARCHAR(255),
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    issue_note VARCHAR(255),
                    worker_account_id VARCHAR(255),
                    PRIMARY KEY (location_id, order_id, sku_id, tenant_id)
                )
                """);
    }

    private void insertFeeSetting(
            String tenantId,
            String warehouseId,
            String status,
            LocalDate effectiveFrom,
            String storagePalletRateAmt,
            String pickBaseRateAmt,
            String packingMaterialRateAmt
    ) {
        writableWmsJdbcTemplate.update("""
                INSERT INTO fee_setting (
                    tenant_id, warehouse_id, status, effective_from,
                    storage_pallet_rate_amt, storage_min_billing_unit, storage_prorata_rule,
                    pick_base_rate_amt, pick_additional_sku_rate_amt,
                    packing_material_rate_amt, special_packaging_surcharge_amt,
                    created_at, updated_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                tenantId,
                warehouseId,
                status,
                effectiveFrom,
                new BigDecimal(storagePalletRateAmt),
                1,
                "DAILY_AVERAGE",
                new BigDecimal(pickBaseRateAmt),
                BigDecimal.ZERO,
                new BigDecimal(packingMaterialRateAmt),
                BigDecimal.ZERO,
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)),
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)),
                "SYSTEM",
                "SYSTEM"
        );
    }

    private void insertLocation(String locationId, String binId, String warehouseId) {
        writableWmsJdbcTemplate.update("""
                INSERT INTO locations (
                    location_id, bin_id, warehouse_id, zone_id, rack_id, worker_account_id, capacity_quantity, is_active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                locationId,
                binId,
                warehouseId,
                "ZONE-01",
                "RACK-01",
                null,
                100,
                true
        );
    }

    private void insertPickingPacking(
            String tenantId,
            String orderId,
            String skuId,
            String locationId,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
        writableWmsJdbcTemplate.update("""
                INSERT INTO picking_packing (
                    sku_id, location_id, tenant_id, order_id,
                    picked_quantity, packed_quantity,
                    created_at, updated_at, created_by, updated_by,
                    started_at, completed_at, issue_note, worker_account_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                skuId,
                locationId,
                tenantId,
                orderId,
                startedAt == null ? 0 : 1,
                completedAt == null ? 0 : 1,
                Timestamp.valueOf(startedAt == null ? LocalDateTime.of(2026, 1, 1, 0, 0) : startedAt),
                Timestamp.valueOf(completedAt == null ? (startedAt == null ? LocalDateTime.of(2026, 1, 1, 0, 0) : startedAt) : completedAt),
                "SYSTEM",
                "SYSTEM",
                startedAt == null ? null : Timestamp.valueOf(startedAt),
                completedAt == null ? null : Timestamp.valueOf(completedAt),
                "",
                "WORKER-001"
        );
    }
}
