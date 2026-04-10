package com.conk.batch.billing.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.conk.batch.billing.domain.BillingDispatchHistory;
import com.conk.batch.billing.domain.BillingDispatchStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class BillingDispatchHistoryRepositoryTest {

    @Autowired
    private BillingDispatchHistoryRepository billingDispatchHistoryRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("기준월로 발행 이력 목록을 조회할 수 있다")
    void findByBillingMonth_success() {
        BillingDispatchHistory first = BillingDispatchHistory.pending("2026-03", "SELLER-001", "billing.monthly.result.v1");
        first.markSuccess();
        BillingDispatchHistory second = BillingDispatchHistory.pending("2026-03", "SELLER-002", "billing.monthly.result.v1");
        second.markFailed("kafka timeout");
        BillingDispatchHistory third = BillingDispatchHistory.pending("2026-02", "SELLER-003", "billing.monthly.result.v1");

        billingDispatchHistoryRepository.save(first);
        billingDispatchHistoryRepository.save(second);
        billingDispatchHistoryRepository.save(third);

        em.flush();
        em.clear();

        List<BillingDispatchHistory> result = billingDispatchHistoryRepository.findByBillingMonth("2026-03");

        assertEquals(2, result.size());
        assertEquals(1, result.stream().filter(history -> history.getDispatchStatus() == BillingDispatchStatus.SUCCESS).count());
        assertEquals(1, result.stream().filter(history -> history.getDispatchStatus() == BillingDispatchStatus.FAILED).count());
    }
}
