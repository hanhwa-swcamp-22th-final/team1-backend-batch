package com.conk.batch.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingDispatchHistoryTest {

    @Test
    @DisplayName("발행 이력 생성 성공: 월, 셀러, 토픽이 설정되고 상태는 PENDING이다")
    void pending_success() {
        BillingDispatchHistory history = BillingDispatchHistory.pending(
                "2026-03",
                "SELLER-001",
                "billing.monthly.result.v1"
        );

        assertEquals("2026-03", history.getBillingMonth());
        assertEquals("SELLER-001", history.getSellerId());
        assertEquals("billing.monthly.result.v1", history.getTopicName());
        assertEquals(BillingDispatchStatus.PENDING, history.getDispatchStatus());
        assertNull(history.getDispatchedAt());
        assertNull(history.getErrorMessage());
    }

    @Test
    @DisplayName("발행 이력 생성 실패: topicName이 비어 있으면 예외가 발생한다")
    void pending_whenTopicNameIsBlank_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                BillingDispatchHistory.pending("2026-03", "SELLER-001", " ")
        );

        assertEquals(BatchErrorCode.INVALID_TOPIC_NAME, exception.getErrorCode());
        assertEquals("topicName must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("발행 이력 상태 변경 성공: 발행 성공 시 SUCCESS와 dispatchedAt이 기록된다")
    void markSuccess_success() {
        BillingDispatchHistory history = BillingDispatchHistory.pending(
                "2026-03",
                "SELLER-001",
                "billing.monthly.result.v1"
        );

        history.markSuccess();

        assertEquals(BillingDispatchStatus.SUCCESS, history.getDispatchStatus());
        assertNotNull(history.getDispatchedAt());
        assertNull(history.getErrorMessage());
    }

    @Test
    @DisplayName("발행 이력 상태 변경 성공: 발행 실패 시 FAILED와 errorMessage가 기록된다")
    void markFailed_success() {
        BillingDispatchHistory history = BillingDispatchHistory.pending(
                "2026-03",
                "SELLER-001",
                "billing.monthly.result.v1"
        );

        history.markFailed("kafka timeout");

        assertEquals(BillingDispatchStatus.FAILED, history.getDispatchStatus());
        assertNotNull(history.getDispatchedAt());
        assertEquals("kafka timeout", history.getErrorMessage());
    }
}
