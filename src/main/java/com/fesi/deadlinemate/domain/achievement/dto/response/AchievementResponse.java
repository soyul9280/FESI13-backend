package com.fesi.deadlinemate.domain.achievement.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record AchievementResponse(
        List<MemberAchievementResponse> members,
        List<WeeklyRateResponse> teamWeeklyRates,
        BigDecimal teamOverallRate
) {
    @Builder
    public record MemberAchievementResponse(
            Long userId,
            String nickname,
            List<WeeklyRateResponse> weeklyRates,
            BigDecimal overallRate,
            int streakDays
    ) {
    }

    @Builder
    public record WeeklyRateResponse(
            int week,
            BigDecimal rate
    ) {
    }
}
