package com.conk.batch.billing.repository;

import com.conk.batch.billing.domain.SellerMonthlyBilling;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerMonthlyBillingRepository extends JpaRepository<SellerMonthlyBilling, Long> {

    void deleteByBillingMonth(String billingMonth);

    List<SellerMonthlyBilling> findByBillingMonth(String billingMonth);
}
