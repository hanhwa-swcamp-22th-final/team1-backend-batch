package com.conk.batch.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

/**
 * occupied bin 일수를 기준으로 보관료를 계산한다.
 */
@Component
public class StorageFeeCalculator {

    public StorageFeeResult calculate(
            int occupiedBinDays,
            BigDecimal storageUnitPrice,
            YearMonth billingMonth
    ) {
        BigDecimal averageOccupiedBins = BigDecimal.valueOf(occupiedBinDays)
                .divide(BigDecimal.valueOf(billingMonth.lengthOfMonth()), 2, RoundingMode.HALF_UP);
        BigDecimal storageFee = storageUnitPrice.multiply(averageOccupiedBins)
                .setScale(0, RoundingMode.HALF_UP);
        return new StorageFeeResult(averageOccupiedBins, storageFee);
    }

    public record StorageFeeResult(BigDecimal averageOccupiedBins, BigDecimal storageFee) {
    }
}
