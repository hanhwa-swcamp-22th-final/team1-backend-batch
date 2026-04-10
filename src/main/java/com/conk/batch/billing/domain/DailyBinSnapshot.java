package com.conk.batch.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일자별 셀러 점유 bin 수 스냅샷이다.
 */
@Getter
@Entity
@Table(
        name = "daily_bin_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_bin_snapshot_date_seller_warehouse",
                columnNames = {"snapshot_date", "seller_id", "warehouse_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyBinSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_bin_snapshot_id", nullable = false)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "seller_id", nullable = false, length = 50)
    private String sellerId;

    @Column(name = "warehouse_id", nullable = false, length = 50)
    private String warehouseId;

    @Column(name = "occupied_bin_count", nullable = false)
    private Integer occupiedBinCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static DailyBinSnapshot of(
            LocalDate snapshotDate,
            String sellerId,
            String warehouseId,
            Integer occupiedBinCount
    ) {
        DailyBinSnapshot snapshot = new DailyBinSnapshot();
        snapshot.snapshotDate = snapshotDate;
        snapshot.sellerId = sellerId;
        snapshot.warehouseId = warehouseId;
        snapshot.occupiedBinCount = occupiedBinCount;
        snapshot.createdAt = LocalDateTime.now();
        return snapshot;
    }
}
