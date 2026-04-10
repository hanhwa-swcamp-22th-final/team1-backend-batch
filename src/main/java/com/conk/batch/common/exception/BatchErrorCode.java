package com.conk.batch.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 배치 서비스에서 사용하는 정산 관련 에러 코드다.
 */
@Getter
@RequiredArgsConstructor
public enum BatchErrorCode implements ErrorCode {

    INVALID_SNAPSHOT_DATE("BILLING-001", "snapshotDate is invalid"),
    INVALID_SELLER_ID("BILLING-002", "sellerId is invalid"),
    INVALID_WAREHOUSE_ID("BILLING-003", "warehouseId is invalid"),
    INVALID_OCCUPIED_BIN_COUNT("BILLING-004", "occupiedBinCount is invalid"),
    WMS_BIN_COUNT_FETCH_FAILED("BILLING-005", "failed to fetch daily bin counts from WMS"),
    MONTHLY_BILLING_CALCULATION_FAILED("BILLING-006", "failed to calculate monthly billing"),
    BILLING_RESULT_PUBLISH_FAILED("BILLING-007", "failed to publish billing result"),
    INVALID_BILLING_MONTH("BILLING-008", "billingMonth is invalid"),
    INVALID_STORAGE_UNIT_PRICE("BILLING-009", "storageUnitPrice is invalid"),
    INVALID_PICK_UNIT_PRICE("BILLING-010", "pickUnitPrice is invalid"),
    INVALID_PACK_UNIT_PRICE("BILLING-011", "packUnitPrice is invalid"),
    FEE_SETTING_FETCH_FAILED("BILLING-012", "failed to fetch fee settings from WMS"),
    PICK_PACK_AGGREGATION_FETCH_FAILED("BILLING-013", "failed to fetch pick-pack aggregations from WMS");

    private final String code;
    private final String message;
}
