package com.fesi.deadlinemate.domain.achievement.controller;


import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fesi.deadlinemate.domain.achievement.dto.response.AchievementRankingResponse;
import com.fesi.deadlinemate.domain.achievement.dto.response.AchievementResponse;
import com.fesi.deadlinemate.domain.achievement.service.AchievementQueryService;
import com.fesi.deadlinemate.global.security.JwtTokenProvider;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AchievementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AchievementQueryService achievementQueryService;

    private String token;
    private AchievementResponse achievementResponse;
    private AchievementRankingResponse rankingResponse;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateAccessToken(1L, "user1@test.com");
        achievementResponse = createAchievementResponse();
        rankingResponse = createRankingResponse();
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/{gatheringId}/achievements")
    class GetAchievements {

        @Test
        @DisplayName("인증된 사용자는 모임 전체 달성률 현황을 조회할 수 있다")
        void getAchievements_success() throws Exception {
            given(achievementQueryService.getAchievements(1L, 1L))
                    .willReturn(achievementResponse);

            mockMvc.perform(get("/api/v1/gatherings/{gatheringId}/achievements", 1L)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.members[0].userId").value(1))
                    .andExpect(jsonPath("$.data.members[0].nickname").value("모임장"))
                    .andExpect(jsonPath("$.data.members[0].weeklyRates[0].week").value(1))
                    .andExpect(jsonPath("$.data.teamOverallRate").value(68.5));
        }

        @Test
        @DisplayName("인증 없이 달성률 현황 조회 시 403을 반환한다")
        void getAchievements_withoutAuth_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/gatherings/{gatheringId}/achievements", 1L))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/{gatheringId}/achievements/ranking")
    class GetRanking {

        @Test
        @DisplayName("인증된 사용자는 달성률 순위를 조회할 수 있다")
        void getRanking_success() throws Exception {
            given(achievementQueryService.getRanking(1L, 1L))
                    .willReturn(rankingResponse);

            mockMvc.perform(get("/api/v1/gatherings/{gatheringId}/achievements/ranking", 1L)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.ranking[0].rank").value(1))
                    .andExpect(jsonPath("$.data.ranking[0].userId").value(1))
                    .andExpect(jsonPath("$.data.ranking[0].nickname").value("모임장"))
                    .andExpect(jsonPath("$.data.ranking[0].overallRate").value(85.0));
        }
    }

    private AchievementResponse createAchievementResponse() {
        return AchievementResponse.builder()
                .members(List.of(
                        AchievementResponse.MemberAchievementResponse.builder()
                                .userId(1L)
                                .nickname("모임장")
                                .weeklyRates(List.of(
                                        AchievementResponse.WeeklyRateResponse.builder()
                                                .week(1)
                                                .rate(BigDecimal.valueOf(100.0))
                                                .build(),
                                        AchievementResponse.WeeklyRateResponse.builder()
                                                .week(2)
                                                .rate(BigDecimal.valueOf(80.0))
                                                .build()
                                ))
                                .overallRate(BigDecimal.valueOf(90.0))
                                .build()
                ))
                .teamWeeklyRates(List.of(
                        AchievementResponse.WeeklyRateResponse.builder()
                                .week(1)
                                .rate(BigDecimal.valueOf(90.0))
                                .build(),
                        AchievementResponse.WeeklyRateResponse.builder()
                                .week(2)
                                .rate(BigDecimal.valueOf(47.0))
                                .build()
                ))
                .teamOverallRate(BigDecimal.valueOf(68.5))
                .build();
    }

    private AchievementRankingResponse createRankingResponse() {
        return AchievementRankingResponse.of(List.of(
                AchievementRankingResponse.RankingItemResponse.builder()
                        .rank(1)
                        .userId(1L)
                        .nickname("모임장")
                        .profileImage("https://profile.test/1.png")
                        .overallRate(BigDecimal.valueOf(85.0))
                        .build(),
                AchievementRankingResponse.RankingItemResponse.builder()
                        .rank(2)
                        .userId(2L)
                        .nickname("멤버")
                        .profileImage("https://profile.test/2.png")
                        .overallRate(BigDecimal.valueOf(75.0))
                        .build()
        ));
    }
}