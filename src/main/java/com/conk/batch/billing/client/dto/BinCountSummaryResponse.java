package com.conk.batch.billing.client.dto;

/**
 * WMS가 반환하는 셀러별 점유 bin 수 응답이다.
 */
public record BinCountSummaryResponse(
        String sellerId,
        String warehouseId,
        Integer occupiedBinCount
) {
}
