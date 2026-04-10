package com.conk.batch.billing.repository;

import com.conk.batch.billing.domain.BillingDispatchHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingDispatchHistoryRepository extends JpaRepository<BillingDispatchHistory, Long> {

    List<BillingDispatchHistory> findByBillingMonth(String billingMonth);
}
