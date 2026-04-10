package com.conk.batch.billing.domain;

/**
 * 월 정산 결과의 처리 상태다.
 */
public enum BillingStatus {
    CALCULATED,
    DISPATCHED,
    PUBLISH_FAILED
}
