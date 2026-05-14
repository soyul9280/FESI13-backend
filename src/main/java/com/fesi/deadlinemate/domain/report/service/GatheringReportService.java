package com.fesi.deadlinemate.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.gathering.event.GatheringMemberEvaluatedEvent;
import com.fesi.deadlinemate.domain.report.dto.WeeklyRateDto;
import com.fesi.deadlinemate.domain.report.entity.GatheringReport;
import com.fesi.deadlinemate.global.common.AchievementRateCalculator;
import com.fesi.deadlinemate.domain.report.repository.GatheringReportRepository;
import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GatheringReportService {
    private final GatheringRepository gatheringRepository;
    private final GatheringMemberRepository gatheringMemberRepository;
    private final TodoRepository todoRepository;
    private final GatheringReportRepository gatheringReportRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createReport(Long gatheringId) {
        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

        if (gathering.getStatus() != GatheringStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.GATHERING_REPORT_NOT_AVAILABLE);
        }

        if (gatheringReportRepository.findByGatheringId(gatheringId).isPresent()) {
            throw new BusinessException(ErrorCode.GATHERING_REPORT_ALREADY_EXISTS);
        }

        List<GatheringMember> activeMembers =
                gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId);

        List<Long> userIds = activeMembers.stream()
                .map(GatheringMember::getUserId)
                .distinct()
                .toList();

        List<Todo> todos = todoRepository
                .findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(gatheringId, userIds);

        Map<Long, List<Todo>> todosByUser = groupByUser(todos);
        Map<Integer, List<Todo>> teamTodosByWeek = groupByWeek(todos);
        Map<Long, Map<Integer, List<Todo>>> todosByUserAndWeek = groupByUserAndWeek(todos);

        List<WeeklyRateDto> teamWeeklyRates = buildWeeklyRates(teamTodosByWeek, gathering.getTotalWeeks());
        BigDecimal teamOverallRate = AchievementRateCalculator.calculateRate(
                todos.stream().filter(Todo::isCompleted).count(),
                todos.size()
        );

        List<MemberReportRow> memberRows = activeMembers.stream()
                .map(member -> buildMemberReportRow(
                        member.getUserId(),
                        gathering.getTotalWeeks(),
                        todosByUser.getOrDefault(member.getUserId(), List.of()),
                        todosByUserAndWeek.getOrDefault(member.getUserId(), Map.of())
                ))
                .toList();

        BigDecimal maxRate = memberRows.stream()
                .map(MemberReportRow::overallRate)
                .max(Comparator.naturalOrder())
                .orElse(null);
        List<Long> mvpUserIds = maxRate == null ? List.of() : memberRows.stream()
                .filter(row -> row.overallRate().compareTo(maxRate) == 0)
                .map(MemberReportRow::userId)
                .toList();

        int maxStreak = memberRows.stream()
                .mapToInt(MemberReportRow::longestStreak)
                .max()
                .orElse(0);
        List<Long> longestStreakUserIds = maxStreak == 0 ? List.of() : memberRows.stream()
                .filter(row -> row.longestStreak() == maxStreak)
                .map(MemberReportRow::userId)
                .toList();

        BigDecimal maxImprovement = memberRows.stream()
                .map(MemberReportRow::improvement)
                .max(Comparator.naturalOrder())
                .orElse(null);
        List<Long> mostImprovedUserIds = (maxImprovement == null || maxImprovement.compareTo(BigDecimal.ZERO) <= 0)
                ? List.of() : memberRows.stream()
                .filter(row -> row.improvement().compareTo(maxImprovement) == 0)
                .map(MemberReportRow::userId)
                .toList();

        List<Long> attendanceUserIds = memberRows.stream()
                .filter(MemberReportRow::attendanceAwardWinner)
                .map(MemberReportRow::userId)
                .toList();

        String weeklyRatesJson = writeWeeklyRates(teamWeeklyRates);

        GatheringReport report = GatheringReport.builder()
                .gatheringId(gatheringId)
                .teamOverallRate(teamOverallRate.setScale(1, RoundingMode.HALF_UP))
                .mvpUserIds(mvpUserIds)
                .longestStreakUserIds(longestStreakUserIds)
                .longestStreakValue(maxStreak)
                .mostImprovedUserIds(mostImprovedUserIds)
                .attendanceUserIds(attendanceUserIds)
                .weeklyRates(weeklyRatesJson)
                .build();

        gatheringReportRepository.save(report);

        memberRows.forEach(row -> {
            BigDecimal delta = calculateReputationDelta(row.overallRate(), row.weeklyRates());
            boolean hasWeeklyPenalty = row.weeklyRates().stream()
                    .anyMatch(r -> r.compareTo(BigDecimal.valueOf(50)) < 0);
            boolean hasConsecutivePenalty = hasConsecutiveFailure(row.weeklyRates());
            eventPublisher.publishEvent(new GatheringMemberEvaluatedEvent(
                    gatheringId, row.userId(), delta, hasWeeklyPenalty, hasConsecutivePenalty
            ));
        });
    }

    private Map<Long, List<Todo>> groupByUser(List<Todo> todos) {
        return todos.stream()
                .collect(Collectors.groupingBy(
                        Todo::getUserId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Integer, List<Todo>> groupByWeek(List<Todo> todos) {
        return todos.stream()
                .collect(Collectors.groupingBy(
                        Todo::getWeekNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, Map<Integer, List<Todo>>> groupByUserAndWeek(List<Todo> todos) {
        return todos.stream()
                .collect(Collectors.groupingBy(
                        Todo::getUserId,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                Todo::getWeekNumber,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));
    }

    private List<WeeklyRateDto> buildWeeklyRates(Map<Integer, List<Todo>> todosByWeek, int totalWeeks) {
        List<WeeklyRateDto> result = new ArrayList<>();

        for (int week = 1; week <= totalWeeks; week++) {
            List<Todo> weeklyTodos = todosByWeek.getOrDefault(week, List.of());

            long totalCount = weeklyTodos.size();
            long completedCount = weeklyTodos.stream()
                    .filter(Todo::isCompleted)
                    .count();

            result.add(new WeeklyRateDto(
                    week,
                    AchievementRateCalculator.calculateRate(completedCount, totalCount)
            ));
        }

        return result;
    }

    private MemberReportRow buildMemberReportRow(
            Long userId,
            int totalWeeks,
            List<Todo> memberTodos,
            Map<Integer, List<Todo>> memberTodosByWeek
    ) {
        long totalTodos = memberTodos.size();
        long completedTodos = memberTodos.stream()
                .filter(Todo::isCompleted)
                .count();

        BigDecimal overallRate = AchievementRateCalculator.calculateRate(completedTodos, totalTodos);

        List<BigDecimal> weeklyRates = new ArrayList<>();
        for (int week = 1; week <= totalWeeks; week++) {
            List<Todo> weeklyTodos = memberTodosByWeek.getOrDefault(week, List.of());

            long weekTotal = weeklyTodos.size();
            long weekCompleted = weeklyTodos.stream()
                    .filter(Todo::isCompleted)
                    .count();

            weeklyRates.add(AchievementRateCalculator.calculateRate(weekCompleted, weekTotal));
        }

        int longestStreak = calculateLongestPerfectStreak(weeklyRates);
        BigDecimal improvement = calculateImprovement(weeklyRates);
        boolean attendanceAwardWinner = isAttendanceAwardWinner(memberTodosByWeek, totalWeeks);

        return new MemberReportRow(
                userId,
                overallRate,
                weeklyRates,
                longestStreak,
                improvement,
                attendanceAwardWinner
        );
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

    private BigDecimal calculateImprovement(List<BigDecimal> weeklyRates) {
        if (weeklyRates.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal firstWeek = weeklyRates.get(0);
        BigDecimal lastWeek = weeklyRates.get(weeklyRates.size() - 1);

        return lastWeek.subtract(firstWeek).setScale(1, RoundingMode.HALF_UP);
    }

    private boolean isAttendanceAwardWinner(Map<Integer, List<Todo>> memberTodosByWeek, int totalWeeks) {
        for (int week = 1; week <= totalWeeks; week++) {
            List<Todo> weeklyTodos = memberTodosByWeek.getOrDefault(week, List.of());

            long completedCount = weeklyTodos.stream()
                    .filter(Todo::isCompleted)
                    .count();

            if (completedCount < 1L) {
                return false;
            }
        }

        return true;
    }

    private String writeWeeklyRates(List<WeeklyRateDto> weeklyRates) {
        try {
            return objectMapper.writeValueAsString(weeklyRates);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_GATHERING_REPORT_DATA);
        }
    }

    private boolean hasConsecutiveFailure(List<BigDecimal> weeklyRates) {
        boolean prevFailed = false;
        for (BigDecimal rate : weeklyRates) {
            boolean failed = rate.compareTo(BigDecimal.valueOf(50)) < 0;
            if (failed && prevFailed) {
                return true;
            }
            prevFailed = failed;
        }
        return false;
    }

    private BigDecimal calculateReputationDelta(BigDecimal overallRate, List<BigDecimal> weeklyRates) {
        BigDecimal delta = BigDecimal.ZERO;

        if (overallRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            delta = delta.add(BigDecimal.valueOf(0.5));
        } else if (overallRate.compareTo(BigDecimal.valueOf(50)) >= 0) {
            delta = delta.add(BigDecimal.valueOf(0.1));
        }

        boolean prevFailed = false;
        boolean hasConsecutiveFailure = false;
        for (BigDecimal weeklyRate : weeklyRates) {
            boolean thisFailed = weeklyRate.compareTo(BigDecimal.valueOf(50)) < 0;
            if (thisFailed) {
                delta = delta.subtract(BigDecimal.valueOf(0.3));
                if (prevFailed) {
                    hasConsecutiveFailure = true;
                }
            }
            prevFailed = thisFailed;
        }

        if (hasConsecutiveFailure) {
            delta = delta.subtract(BigDecimal.valueOf(0.5));
        }

        return delta.setScale(1, RoundingMode.HALF_UP);
    }

    private record MemberReportRow(
            Long userId,
            BigDecimal overallRate,
            List<BigDecimal> weeklyRates,
            int longestStreak,
            BigDecimal improvement,
            boolean attendanceAwardWinner
    ) {
    }
}
