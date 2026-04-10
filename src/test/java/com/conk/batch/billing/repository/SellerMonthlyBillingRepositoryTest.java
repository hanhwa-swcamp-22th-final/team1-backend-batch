package com.conk.batch.billing.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.conk.batch.billing.domain.SellerMonthlyBilling;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SellerMonthlyBillingRepositoryTest {

    @Autowired
    private SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("기준월로 월 정산 결과를 조회할 수 있다")
    void findByBillingMonth_success() {
        sellerMonthlyBillingRepository.save(createBilling("2026-03", "SELLER-001", "WH-001"));
        sellerMonthlyBillingRepository.save(createBilling("2026-03", "SELLER-002", "WH-001"));
        sellerMonthlyBillingRepository.save(createBilling("2026-02", "SELLER-003", "WH-001"));

        em.flush();
        em.clear();

        List<SellerMonthlyBilling> result = sellerMonthlyBillingRepository.findByBillingMonth("2026-03");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(billing -> billing.getSellerId().equals("SELLER-001")));
        assertTrue(result.stream().anyMatch(billing -> billing.getSellerId().equals("SELLER-002")));
    }

    @Test
    @DisplayName("기준월로 월 정산 결과를 삭제할 수 있다")
    void deleteByBillingMonth_success() {
        sellerMonthlyBillingRepository.save(createBilling("2026-03", "SELLER-001", "WH-001"));
        sellerMonthlyBillingRepository.save(createBilling("2026-03", "SELLER-002", "WH-001"));
        sellerMonthlyBillingRepository.save(createBilling("2026-02", "SELLER-003", "WH-001"));

        em.flush();
        em.clear();

        sellerMonthlyBillingRepository.deleteByBillingMonth("2026-03");
        em.flush();
        em.clear();

        List<SellerMonthlyBilling> result = sellerMonthlyBillingRepository.findAll();
        assertEquals(1, result.size());
        assertEquals("SELLER-003", result.get(0).getSellerId());
    }

    private SellerMonthlyBilling createBilling(String billingMonth, String sellerId, String warehouseId) {
        return SellerMonthlyBilling.calculated(
                billingMonth,
                sellerId,
                warehouseId,
                87,
                new BigDecimal("2.81"),
                new BigDecimal("80085"),
                3,
                new BigDecimal("7500"),
                1,
                new BigDecimal("2500")
        );
    }
}
