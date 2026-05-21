package com.fesi.deadlinemate.domain.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record GatheringReportResponse(
        GatheringSummaryResponse gathering,
        BigDecimal teamOverallRate,
        List<WeeklyRateResponse> weeklyRates,
        List<MemberResultResponse> memberResults,
        AwardsResponse awards
) {
    @Builder
    public record GatheringSummaryResponse(
            String title,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    @Builder
    public record WeeklyRateResponse(
            int week,
            BigDecimal rate
    ) {
    }

    @Builder
    public record MemberResultResponse(
            Long userId,
            String nickname,
            BigDecimal overallRate,
            int longestStreak,
            long completedTodos,
            long totalTodos,
            List<BigDecimal> weeklyRates
    ) {
    }

    @Builder
    public record AwardsResponse(
            List<UserAwardResponse> mvp,
            List<StreakAwardResponse> longestStreak,
            List<UserAwardResponse> mostImproved,
            List<UserAwardResponse> attendance
    ) {
    }


    @Builder
    public record UserAwardResponse(
            Long userId,
            String nickname
    ) {
    }

    @Builder
    public record StreakAwardResponse(
            Long userId,
            String nickname,
            int streak
    ) {
    }
}