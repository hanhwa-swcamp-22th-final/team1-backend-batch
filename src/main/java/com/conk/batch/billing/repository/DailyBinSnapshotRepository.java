package com.conk.batch.billing.repository;

import com.conk.batch.billing.domain.DailyBinSnapshot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyBinSnapshotRepository extends JpaRepository<DailyBinSnapshot, Long> {

    void deleteBySnapshotDate(LocalDate snapshotDate);

    List<DailyBinSnapshot> findBySnapshotDateBetween(LocalDate startDate, LocalDate endDate);
}
