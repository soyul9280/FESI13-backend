package com.fesi.deadlinemate.global.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AchievementRateCalculator {

    private AchievementRateCalculator() {}

    public static BigDecimal calculateRate(long completedCount, long totalCount) {
        if (totalCount == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
    }
}
