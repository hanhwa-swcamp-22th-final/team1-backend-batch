package com.conk.batch.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.repository.MonthlyFeeSnapshotRepository;
import com.conk.batch.billing.service.MonthlyFeeSnapshotService;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MonthlyFeeSnapshotIntegrationTest {

    @Autowired
    private MonthlyFeeSnapshotService monthlyFeeSnapshotService;

    @Autowired
    private MonthlyFeeSnapshotRepository monthlyFeeSnapshotRepository;

    @Autowired
    @Qualifier("wmsReadDataSourceProperties")
    private DataSourceProperties wmsReadDataSourceProperties;

    private JdbcTemplate writableWmsJdbcTemplate;

    @BeforeEach
    void setUp() {
        writableWmsJdbcTemplate = new JdbcTemplate(createWritableWmsDataSource());
        resetWmsReadSchema();
        monthlyFeeSnapshotRepository.deleteAll();
        monthlyFeeSnapshotRepository.flush();
    }

    @Test
    @DisplayName("월별 요금 snapshot 저장 전체 흐름이 정상 동작한다")
    void captureMonthlyFeeSnapshots_flowSuccess() {
        insertFeeSetting("SELLER-001", "WH-001", "ACTIVE", LocalDate.of(2026, 2, 1), "28000", "2400", "2300");
        insertFeeSetting("SELLER-001", "WH-001", "ACTIVE", LocalDate.of(2026, 3, 15), "28500", "2500", "2500");
        insertFeeSetting("SELLER-002", "WH-001", "ACTIVE", LocalDate.of(2026, 2, 20), "27000", "2300", "2100");

        monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(YearMonth.of(2026, 3));

        List<MonthlyFeeSnapshot> rows = monthlyFeeSnapshotRepository.findAll();
        assertThat(rows)
                .extracting(
                        MonthlyFeeSnapshot::getBillingMonth,
                        MonthlyFeeSnapshot::getSellerId,
                        MonthlyFeeSnapshot::getWarehouseId,
                        MonthlyFeeSnapshot::getStorageUnitPrice,
                        MonthlyFeeSnapshot::getPickUnitPrice,
                        MonthlyFeeSnapshot::getPackUnitPrice
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("2026-03", "SELLER-001", "WH-001", new BigDecimal("28500.00"), new BigDecimal("2500.00"), new BigDecimal("2500.00")),
                        org.assertj.core.groups.Tuple.tuple("2026-03", "SELLER-002", "WH-001", new BigDecimal("27000.00"), new BigDecimal("2300.00"), new BigDecimal("2100.00"))
                );
    }

    @Test
    @DisplayName("월별 요금 snapshot 저장 실패: WMS 요금 설정 데이터가 잘못되면 기존 snapshot 데이터는 유지된다")
    void captureMonthlyFeeSnapshots_whenFeeSettingIsInvalid_thenKeepExistingRows() {
        monthlyFeeSnapshotRepository.saveAndFlush(MonthlyFeeSnapshot.of(
                "2026-03",
                "SELLER-LEGACY",
                "WH-001",
                new BigDecimal("20000"),
                new BigDecimal("1000"),
                new BigDecimal("900")
        ));
        insertFeeSetting("SELLER-001", "WH-001", "ACTIVE", LocalDate.of(2026, 3, 1), "-1", "2500", "2500");

        assertThatThrownBy(() -> monthlyFeeSnapshotService.captureMonthlyFeeSnapshots(YearMonth.of(2026, 3)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(BatchErrorCode.INVALID_STORAGE_UNIT_PRICE);
                    assertThat(exception.getMessage()).isEqualTo("storageUnitPrice must be greater than or equal to 0");
                });

        List<MonthlyFeeSnapshot> rows = monthlyFeeSnapshotRepository.findAll();
        assertThat(rows)
                .extracting(
                        MonthlyFeeSnapshot::getBillingMonth,
                        MonthlyFeeSnapshot::getSellerId,
                        MonthlyFeeSnapshot::getWarehouseId,
                        MonthlyFeeSnapshot::getStorageUnitPrice
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("2026-03", "SELLER-LEGACY", "WH-001", new BigDecimal("20000"))
                );
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
}
