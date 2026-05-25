package com.fesi.deadlinemate.domain.todo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record MyTodoListResponse(
        List<MyTodoItemResponse> todos,
        BigDecimal weeklyAchievementRate,
        BigDecimal overallAchievementRate,
        int streakDays
) {
    public static MyTodoListResponse of(
            List<MyTodoItemResponse> todos,
            BigDecimal weeklyAchievementRate,
            BigDecimal overallAchievementRate,
            int streakDays
    ) {
        return MyTodoListResponse.builder()
                .todos(todos)
                .weeklyAchievementRate(weeklyAchievementRate)
                .overallAchievementRate(overallAchievementRate)
                .streakDays(streakDays)
                .build();
    }

    @Builder
    public record MyTodoItemResponse(
            Long id,
            int week,
            String content,
            boolean isCompleted,
            LocalDateTime createdAt
    ) {
    }
}
