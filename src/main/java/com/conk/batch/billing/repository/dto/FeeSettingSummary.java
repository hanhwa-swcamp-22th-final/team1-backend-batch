package com.conk.batch.billing.repository.dto;

import java.math.BigDecimal;

/**
 * 기준월에 적용되는 셀러별 단가 요약이다.
 */
public record FeeSettingSummary(
        String sellerId,
        String warehouseId,
        BigDecimal storageUnitPrice,
        BigDecimal pickUnitPrice,
        BigDecimal packUnitPrice
) {
}
