package com.conk.batch.billing.repository.dto;

/**
 * 기준월의 피킹/패킹 집계 결과다.
 */
public record PickPackMonthlyAggregation(
        String sellerId,
        String warehouseId,
        int pickCount,
        int packCount
) {

    public static PickPackMonthlyAggregation empty(String sellerId, String warehouseId) {
        return new PickPackMonthlyAggregation(sellerId, warehouseId, 0, 0);
    }
}
