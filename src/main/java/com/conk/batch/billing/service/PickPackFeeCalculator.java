package com.conk.batch.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * 건당 단가 기반 피킹/패킹 비용 계산기다.
 */
@Component
public class PickPackFeeCalculator {

    public BigDecimal calculate(int count, BigDecimal unitPrice) {
        return unitPrice.multiply(BigDecimal.valueOf(count))
                .setScale(0, RoundingMode.HALF_UP);
    }
}
