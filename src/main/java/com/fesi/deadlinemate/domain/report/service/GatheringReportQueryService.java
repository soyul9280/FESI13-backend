package com.fesi.deadlinemate.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.report.dto.GatheringReportResponse;
import com.fesi.deadlinemate.domain.report.dto.WeeklyRateDto;
import com.fesi.deadlinemate.domain.report.entity.GatheringReport;
import com.fesi.deadlinemate.domain.report.repository.GatheringReportRepository;
import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.global.common.AchievementRateCalculator;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatheringReportQueryService {

    private final GatheringRepository gatheringRepository;
    private final GatheringMemberRepository gatheringMemberRepository;
    private final TodoRepository todoRepository;
    private final GatheringReportRepository gatheringReportRepository;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    public GatheringReportResponse getReport(Long gatheringId, Long requesterId) {
        Gathering gathering = findGathering(gatheringId);
        validateActiveMember(gatheringId, requesterId);
        validateCompletedGathering(gathering);

        GatheringReport report = gatheringReportRepository.findByGatheringId(gatheringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_REPORT_NOT_FOUND));

        List<GatheringMember> activeMembers =
                gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId);

        List<Long> userIds = activeMembers.stream()
                .map(GatheringMember::getUserId)
                .distinct()
                .toList();

        Map<Long, UserInfo> userMap = loadUsers(userIds);

        List<Todo> todos = todoRepository
                .findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(gatheringId, userIds);

        List<GatheringReportResponse.MemberResultResponse> memberResults = activeMembers.stream()
                .map(member -> toMemberResult(member.getUserId(), gathering.getTotalWeeks(), todos, userMap))
                .toList();

        List<GatheringReportResponse.WeeklyRateResponse> teamWeeklyRates = parseWeeklyRates(report.getWeeklyRates());

        GatheringReportResponse.AwardsResponse awards = GatheringReportResponse.AwardsResponse.builder()
                .mvp(toUserAwards(report.getMvpUserIds(), userMap))
                .longestStreak(toStreakAwards(report.getLongestStreakUserIds(), report.getLongestStreakValue(), userMap))
                .mostImproved(toUserAwards(report.getMostImprovedUserIds(), userMap))
                .attendance(toUserAwards(report.getAttendanceUserIds(), userMap))
                .build();

        return GatheringReportResponse.builder()
                .gathering(GatheringReportResponse.GatheringSummaryResponse.builder()
                        .title(gathering.getTitle())
                        .startDate(gathering.getStartDate())
                        .endDate(gathering.getEndDate())
                        .build())
                .teamOverallRate(report.getTeamOverallRate().setScale(1, RoundingMode.HALF_UP))
                .weeklyRates(teamWeeklyRates)
                .memberResults(memberResults)
                .awards(awards)
                .build();
    }

    private Gathering findGathering(Long gatheringId) {
        return gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));
    }

    private void validateActiveMember(Long gatheringId, Long userId) {
        boolean isActiveMember = gatheringMemberRepository
                .existsByGatheringIdAndUserIdAndIsActiveTrue(gatheringId, userId);

        if (!isActiveMember) {
            throw new BusinessException(ErrorCode.NOT_A_MEMBER);
        }
    }

    private void validateCompletedGathering(Gathering gathering) {
        if (gathering.getStatus() != GatheringStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.GATHERING_REPORT_NOT_AVAILABLE);
        }
    }

    private GatheringReportResponse.MemberResultResponse toMemberResult(
            Long userId,
            int totalWeeks,
            List<Todo> allTodos,
            Map<Long, UserInfo> userMap
    ) {
        List<Todo> memberTodos = allTodos.stream()
                .filter(todo -> todo.getUserId().equals(userId))
                .toList();

        long totalTodos = memberTodos.size();
        long completedTodos = memberTodos.stream()
                .filter(Todo::isCompleted)
                .count();

        BigDecimal overallRate = AchievementRateCalculator.calculateRate(completedTodos, totalTodos);

        List<BigDecimal> weeklyRates = new ArrayList<>();
        for (int week = 1; week <= totalWeeks; week++) {
            final int targetWeek = week;

            List<Todo> weeklyTodos = memberTodos.stream()
                    .filter(todo -> todo.getWeekNumber() == targetWeek)
                    .toList();

            long weekTotal = weeklyTodos.size();
            long weekCompleted = weeklyTodos.stream()
                    .filter(Todo::isCompleted)
                    .count();

            weeklyRates.add(AchievementRateCalculator.calculateRate(weekCompleted, weekTotal));
        }

        int longestStreak = calculateLongestPerfectStreak(weeklyRates);

        UserInfo user = userMap.get(userId);

        return GatheringReportResponse.MemberResultResponse.builder()
                .userId(userId)
                .nickname(user != null ? user.getNickname() : null)
                .overallRate(overallRate)
                .longestStreak(longestStreak)
                .completedTodos(completedTodos)
                .totalTodos(totalTodos)
                .weeklyRates(weeklyRates)
                .build();
    }

    private int calculateLongestPerfectStreak(List<BigDecimal> weeklyRates) {
        int max = 0;
        int current = 0;

        for (BigDecimal weeklyRate : weeklyRates) {
            if (weeklyRate.compareTo(BigDecimal.valueOf(100.0)) == 0) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }
        return max;
    }

    private List<GatheringReportResponse.WeeklyRateResponse> parseWeeklyRates(String weeklyRatesJson) {
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, WeeklyRateDto.class);

            List<WeeklyRateDto> payloads = objectMapper.readValue(weeklyRatesJson, type);

            return payloads.stream()
                    .map(payload -> GatheringReportResponse.WeeklyRateResponse.builder()
                            .week(payload.week())
                            .rate(payload.rate().setScale(1, RoundingMode.HALF_UP))
                            .build())
                    .toList();
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_GATHERING_REPORT_DATA);
        }
    }

    private List<GatheringReportResponse.UserAwardResponse> toUserAwards(
            List<Long> userIds,
            Map<Long, UserInfo> userMap
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userIds.stream()
                .map(userId -> {
                    UserInfo user = userMap.get(userId);
                    if (user == null) {
                        user = userClient.findById(userId);
                    }
                    return GatheringReportResponse.UserAwardResponse.builder()
                            .userId(userId)
                            .nickname(user != null ? user.getNickname() : null)
                            .build();
                })
                .toList();
    }

    private List<GatheringReportResponse.StreakAwardResponse> toStreakAwards(
            List<Long> userIds,
            int streakValue,
            Map<Long, UserInfo> userMap
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userIds.stream()
                .map(userId -> {
                    UserInfo user = userMap.get(userId);
                    if (user == null) {
                        user = userClient.findById(userId);
                    }
                    return GatheringReportResponse.StreakAwardResponse.builder()
                            .userId(userId)
                            .nickname(user != null ? user.getNickname() : null)
                            .streak(streakValue)
                            .build();
                })
                .toList();
    }

    private Map<Long, UserInfo> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, UserInfo> result = new HashMap<>();
        for (Long userId : userIds) {
            result.put(userId, userClient.findById(userId));
        }
        return result;
    }
}