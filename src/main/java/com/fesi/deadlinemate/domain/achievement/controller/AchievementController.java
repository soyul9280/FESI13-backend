package com.fesi.deadlinemate.domain.achievement.controller;

import com.fesi.deadlinemate.domain.achievement.dto.response.AchievementRankingResponse;
import com.fesi.deadlinemate.domain.achievement.dto.response.AchievementResponse;
import com.fesi.deadlinemate.domain.achievement.service.AchievementQueryService;
import com.fesi.deadlinemate.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gatherings")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementQueryService achievementQueryService;

    @GetMapping("/{gatheringId}/achievements")
    public ApiResponse<AchievementResponse> getAchievements(
            @PathVariable Long gatheringId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(
                achievementQueryService.getAchievements(gatheringId, userId)
        );
    }

    @GetMapping("/{gatheringId}/achievements/ranking")
    public ApiResponse<AchievementRankingResponse> getRanking(
            @PathVariable Long gatheringId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(
                achievementQueryService.getRanking(gatheringId, userId)
        );
    }
}
