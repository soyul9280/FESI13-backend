package com.fesi.deadlinemate.domain.report.controller;


import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fesi.deadlinemate.domain.report.dto.GatheringReportResponse;
import com.fesi.deadlinemate.domain.report.service.GatheringReportQueryService;
import com.fesi.deadlinemate.global.security.JwtTokenProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class GatheringReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private GatheringReportQueryService gatheringReportQueryService;

    private String token;
    private GatheringReportResponse reportResponse;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateAccessToken(1L, "user1@test.com");
        reportResponse = createReportResponse();
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/{gatheringId}/report")
    class GetReport {

        @Test
        @DisplayName("인증된 사용자는 모임 리포트를 조회할 수 있다")
        void getReport_success() throws Exception {
            given(gatheringReportQueryService.getReport(1L, 1L))
                    .willReturn(reportResponse);

            mockMvc.perform(get("/api/v1/gatherings/{gatheringId}/report", 1L)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.gathering.title").value("React 완전 정복 스터디"))
                    .andExpect(jsonPath("$.data.teamOverallRate").value(82.5))
                    .andExpect(jsonPath("$.data.memberResults[0].userId").value(1))
                    .andExpect(jsonPath("$.data.awards.mvp[0].nickname").value("모임장"));
        }

        @Test
        @DisplayName("인증 없이 모임 리포트 조회 시 403을 반환한다")
        void getReport_withoutAuth_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/gatherings/{gatheringId}/report", 1L))
                    .andExpect(status().isForbidden());
        }
    }

    private GatheringReportResponse createReportResponse() {
        return GatheringReportResponse.builder()
                .gathering(GatheringReportResponse.GatheringSummaryResponse.builder()
                        .title("React 완전 정복 스터디")
                        .startDate(LocalDate.of(2026, 4, 1))
                        .endDate(LocalDate.of(2026, 5, 1))
                        .build())
                .teamOverallRate(BigDecimal.valueOf(82.5))
                .weeklyRates(List.of(
                        GatheringReportResponse.WeeklyRateResponse.builder()
                                .week(1)
                                .rate(BigDecimal.valueOf(80.0))
                                .build(),
                        GatheringReportResponse.WeeklyRateResponse.builder()
                                .week(2)
                                .rate(BigDecimal.valueOf(85.0))
                                .build()
                ))
                .memberResults(List.of(
                        GatheringReportResponse.MemberResultResponse.builder()
                                .userId(1L)
                                .nickname("모임장")
                                .overallRate(BigDecimal.valueOf(90.0))
                                .longestStreak(2)
                                .completedTodos(9)
                                .totalTodos(10)
                                .weeklyRates(List.of(
                                        BigDecimal.valueOf(100.0),
                                        BigDecimal.valueOf(80.0)
                                ))
                                .build()
                ))
                .awards(GatheringReportResponse.AwardsResponse.builder()
                        .mvp(List.of(GatheringReportResponse.UserAwardResponse.builder()
                                .userId(1L)
                                .nickname("모임장")
                                .build()))
                        .longestStreak(List.of(GatheringReportResponse.StreakAwardResponse.builder()
                                .userId(1L)
                                .nickname("모임장")
                                .streak(2)
                                .build()))
                        .mostImproved(List.of(GatheringReportResponse.UserAwardResponse.builder()
                                .userId(2L)
                                .nickname("멤버")
                                .build()))
                        .attendance(List.of(GatheringReportResponse.UserAwardResponse.builder()
                                .userId(1L)
                                .nickname("모임장")
                                .build()))
                        .build())
                .build();
    }
}