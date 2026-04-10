package com.conk.batch.billing.domain;

/**
 * Kafka 발행 이력의 처리 상태다.
 */
public enum BillingDispatchStatus {
    PENDING,
    SUCCESS,
    FAILED
}
