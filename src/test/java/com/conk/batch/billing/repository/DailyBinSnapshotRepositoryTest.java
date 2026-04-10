package com.conk.batch.billing.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.conk.batch.billing.domain.DailyBinSnapshot;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class DailyBinSnapshotRepositoryTest {

    @Autowired
    private DailyBinSnapshotRepository dailyBinSnapshotRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("날짜 범위로 일별 bin snapshot 목록을 조회할 수 있다")
    void findBySnapshotDateBetween_success() {
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 3, 31), "SELLER-OLD", 1));
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 4, 1), "SELLER-001", 3));
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 4, 2), "SELLER-002", 1));
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 4, 3), "SELLER-LATE", 2));

        em.flush();
        em.clear();

        List<DailyBinSnapshot> result = dailyBinSnapshotRepository.findBySnapshotDateBetween(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2)
        );

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(snapshot -> snapshot.getSellerId().equals("SELLER-001")));
        assertTrue(result.stream().anyMatch(snapshot -> snapshot.getSellerId().equals("SELLER-002")));
    }

    @Test
    @DisplayName("조건에 맞는 snapshot이 없으면 빈 목록을 반환한다")
    void findBySnapshotDateBetween_whenNoMatch_thenReturnEmpty() {
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 3, 31), "SELLER-OLD", 1));

        em.flush();
        em.clear();

        List<DailyBinSnapshot> result = dailyBinSnapshotRepository.findBySnapshotDateBetween(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("기준 날짜로 snapshot을 삭제할 수 있다")
    void deleteBySnapshotDate_success() {
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 4, 10), "SELLER-001", 3));
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 4, 10), "SELLER-002", 1));
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 4, 11), "SELLER-003", 2));

        em.flush();
        em.clear();

        dailyBinSnapshotRepository.deleteBySnapshotDate(LocalDate.of(2026, 4, 10));
        em.flush();
        em.clear();

        List<DailyBinSnapshot> result = dailyBinSnapshotRepository.findAll();
        assertEquals(1, result.size());
        assertEquals("SELLER-003", result.get(0).getSellerId());
    }

    @Test
    @DisplayName("같은 날짜, 셀러, 창고 조합으로 snapshot을 저장하면 예외가 발생한다")
    void save_whenDuplicateDateSellerWarehouse_thenThrow() {
        dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 4, 10), "SELLER-001", 3));
        em.flush();
        em.clear();

        assertThrows(DataIntegrityViolationException.class, () -> {
            dailyBinSnapshotRepository.save(createSnapshot(LocalDate.of(2026, 4, 10), "SELLER-001", 4));
            em.flush();
        });
    }

    private DailyBinSnapshot createSnapshot(LocalDate snapshotDate, String sellerId, int occupiedBinCount) {
        return DailyBinSnapshot.of(snapshotDate, sellerId, "WH-001", occupiedBinCount);
    }
}
