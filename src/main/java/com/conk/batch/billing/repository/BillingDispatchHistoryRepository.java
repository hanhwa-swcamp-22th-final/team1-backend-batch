package com.conk.batch.billing.repository;

import com.conk.batch.billing.domain.BillingDispatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingDispatchHistoryRepository extends JpaRepository<BillingDispatchHistory, Long> {
}
