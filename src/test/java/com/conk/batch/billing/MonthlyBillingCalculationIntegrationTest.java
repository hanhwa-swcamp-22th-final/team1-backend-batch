package com.conk.batch.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.conk.batch.billing.domain.BillingDispatchHistory;
import com.conk.batch.billing.domain.BillingDispatchStatus;
import com.conk.batch.billing.domain.BillingStatus;
import com.conk.batch.billing.domain.DailyBinSnapshot;
import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.domain.SellerMonthlyBilling;
import com.conk.batch.billing.publisher.BillingResultEventPublisher;
import com.conk.batch.billing.repository.BillingDispatchHistoryRepository;
import com.conk.batch.billing.repository.DailyBinSnapshotRepository;
import com.conk.batch.billing.repository.MonthlyFeeSnapshotRepository;
import com.conk.batch.billing.repository.SellerMonthlyBillingRepository;
import com.conk.batch.billing.service.MonthlyBillingCalculationService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MonthlyBillingCalculationIntegrationTest {

    @Autowired
    private MonthlyBillingCalculationService monthlyBillingCalculationService;

    @Autowired
    private DailyBinSnapshotRepository dailyBinSnapshotRepository;

    @Autowired
    private MonthlyFeeSnapshotRepository monthlyFeeSnapshotRepository;

    @Autowired
    private SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    @Autowired
    private BillingDispatchHistoryRepository billingDispatchHistoryRepository;

    @Autowired
    @Qualifier("wmsReadDataSourceProperties")
    private DataSourceProperties wmsReadDataSourceProperties;

    @MockitoBean
    private BillingResultEventPublisher billingResultEventPublisher;

    private JdbcTemplate writableWmsJdbcTemplate;

    @BeforeEach
    void setUp() {
        writableWmsJdbcTemplate = new JdbcTemplate(createWritableWmsDataSource());
        resetWmsReadSchema();
        billingDispatchHistoryRepository.deleteAll();
        billingDispatchHistoryRepository.flush();
        sellerMonthlyBillingRepository.deleteAll();
        sellerMonthlyBillingRepository.flush();
        monthlyFeeSnapshotRepository.deleteAll();
        monthlyFeeSnapshotRepository.flush();
        dailyBinSnapshotRepository.deleteAll();
        dailyBinSnapshotRepository.flush();
    }

    @Test
    @DisplayName("월 정산 계산과 발행 전체 흐름이 정상 동작한다")
    void calculateAndPublish_flowSuccess() {
        YearMonth billingMonth = YearMonth.of(2026, 3);
        insertFeeSetting("SELLER-001", "WH-001", LocalDate.of(2026, 3, 1), "28500", "2500", "2500");
        insertFeeSetting("SELLER-002", "WH-002", LocalDate.of(2026, 2, 1), "30000", "2000", "1800");
        insertLocation("LOC-A-01-01", "BIN-A-01-01", "WH-001");
        insertLocation("LOC-A-01-02", "BIN-A-01-02", "WH-001");
        insertLocation("LOC-B-01-01", "BIN-B-01-01", "WH-002");
        insertPickingPacking("SELLER-001", "ORD-001", "SKU-001", "LOC-A-01-01",
                LocalDateTime.of(2026, 3, 5, 9, 0), LocalDateTime.of(2026, 3, 6, 18, 0));
        insertPickingPacking("SELLER-001", "ORD-001", "SKU-002", "LOC-A-01-02",
                LocalDateTime.of(2026, 3, 5, 9, 5), null);
        insertPickingPacking("SELLER-002", "ORD-010", "SKU-001", "LOC-B-01-01",
                LocalDateTime.of(2026, 3, 7, 8, 0), LocalDateTime.of(2026, 3, 8, 9, 0));
        dailyBinSnapshotRepository.saveAllAndFlush(List.of(
                DailyBinSnapshot.of(LocalDate.of(2026, 3, 1), "SELLER-001", "WH-001", 31),
                DailyBinSnapshot.of(LocalDate.of(2026, 3, 2), "SELLER-001", "WH-001", 31),
                DailyBinSnapshot.of(LocalDate.of(2026, 3, 1), "SELLER-002", "WH-002", 31)
        ));

        List<SellerMonthlyBilling> result = monthlyBillingCalculationService.calculateAndPublish(billingMonth);

        List<SellerMonthlyBilling> savedBillings = sellerMonthlyBillingRepository.findByBillingMonth("2026-03");
        List<BillingDispatchHistory> histories = billingDispatchHistoryRepository.findByBillingMonth("2026-03");

        assertThat(result).hasSize(2);
        assertThat(savedBillings)
                .extracting(
                        SellerMonthlyBilling::getSellerId,
                        SellerMonthlyBilling::getWarehouseId,
                        SellerMonthlyBilling::getOccupiedBinDays,
                        SellerMonthlyBilling::getPickCount,
                        SellerMonthlyBilling::getPackCount,
                        SellerMonthlyBilling::getTotalFee,
                        SellerMonthlyBilling::getStatus
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("SELLER-001", "WH-001", 62, 2, 1, new BigDecimal("64500"), BillingStatus.DISPATCHED),
                        org.assertj.core.groups.Tuple.tuple("SELLER-002", "WH-002", 31, 1, 1, new BigDecimal("33800"), BillingStatus.DISPATCHED)
                );
        assertThat(histories)
                .extracting(BillingDispatchHistory::getSellerId, BillingDispatchHistory::getDispatchStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("SELLER-001", BillingDispatchStatus.SUCCESS),
                        org.assertj.core.groups.Tuple.tuple("SELLER-002", BillingDispatchStatus.SUCCESS)
                );
        verify(billingResultEventPublisher, times(2)).publish(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("월 정산 발행 실패 시 정산 결과와 실패 이력이 저장된다")
    void calculateAndPublish_whenPublishFails_thenKeepBillingAndFailureHistory() {
        YearMonth billingMonth = YearMonth.of(2026, 3);
        insertFeeSetting("SELLER-001", "WH-001", LocalDate.of(2026, 3, 1), "28500", "2500", "2500");
        insertLocation("LOC-A-01-01", "BIN-A-01-01", "WH-001");
        insertPickingPacking("SELLER-001", "ORD-001", "SKU-001", "LOC-A-01-01",
                LocalDateTime.of(2026, 3, 5, 9, 0), LocalDateTime.of(2026, 3, 6, 18, 0));
        dailyBinSnapshotRepository.saveAndFlush(
                DailyBinSnapshot.of(LocalDate.of(2026, 3, 1), "SELLER-001", "WH-001", 31)
        );
        doThrow(new BusinessException(BatchErrorCode.BILLING_RESULT_PUBLISH_FAILED, "kafka failed"))
                .when(billingResultEventPublisher)
                .publish(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        List<SellerMonthlyBilling> result = monthlyBillingCalculationService.calculateAndPublish(billingMonth);

        List<SellerMonthlyBilling> savedBillings = sellerMonthlyBillingRepository.findByBillingMonth("2026-03");
        List<BillingDispatchHistory> histories = billingDispatchHistoryRepository.findByBillingMonth("2026-03");

        assertThat(result).hasSize(1);
        assertThat(savedBillings)
                .extracting(SellerMonthlyBilling::getSellerId, SellerMonthlyBilling::getStatus)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("SELLER-001", BillingStatus.PUBLISH_FAILED));
        assertThat(histories)
                .extracting(
                        BillingDispatchHistory::getSellerId,
                        BillingDispatchHistory::getDispatchStatus,
                        BillingDispatchHistory::getErrorMessage
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SELLER-001", BillingDispatchStatus.FAILED, "kafka failed")
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
                ) VALUES (?, ?, 'ACTIVE', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                tenantId,
                warehouseId,
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
