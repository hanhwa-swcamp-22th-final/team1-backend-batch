package com.conk.batch.billing.repository;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyFeeSnapshotRepository extends JpaRepository<MonthlyFeeSnapshot, Long> {

    void deleteByBillingMonth(String billingMonth);

    List<MonthlyFeeSnapshot> findByBillingMonth(String billingMonth);
}
