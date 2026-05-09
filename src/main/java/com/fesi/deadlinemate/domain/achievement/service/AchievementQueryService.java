package com.fesi.deadlinemate.domain.achievement.service;

import com.fesi.deadlinemate.domain.achievement.dto.response.AchievementRankingResponse;
import com.fesi.deadlinemate.domain.achievement.dto.response.AchievementResponse;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AchievementQueryService {

    private final GatheringRepository gatheringRepository;
    private final GatheringMemberRepository gatheringMemberRepository;
    private final TodoRepository todoRepository;
    private final UserClient userClient;

    public AchievementResponse getAchievements(Long gatheringId, Long requesterId) {
        Gathering gathering = findGathering(gatheringId);
        validateActiveMember(gatheringId, requesterId);

        List<GatheringMember> activeMembers =
                gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId);

        Set<Long> activeUserIds = activeMembers.stream()
                .map(GatheringMember::getUserId)
                .collect(Collectors.toSet());

        List<Todo> todos = todoRepository.findByGatheringIdOrderByWeekNumberAscCreatedAtAsc(gatheringId).stream()
                .filter(todo -> activeUserIds.contains(todo.getUserId()))
                .toList();

        Map<Long, UserInfo> userMap = loadUsers(
                activeMembers.stream()
                        .map(GatheringMember::getUserId)
                        .distinct()
                        .toList()
        );

        Map<Long, List<Todo>> todosByUser = groupByUser(todos);
        Map<Integer, List<Todo>> teamTodosByWeek = groupByWeek(todos);
        Map<Long, Map<Integer, List<Todo>>> todosByUserAndWeek = groupByUserAndWeek(todos);

        List<AchievementResponse.MemberAchievementResponse> memberResponses = activeMembers.stream()
                .map(member -> {
                    Long userId = member.getUserId();
                    UserInfo user = userMap.get(userId);

                    List<Todo> memberTodos = todosByUser.getOrDefault(userId, List.of());
                    Map<Integer, List<Todo>> memberWeekMap = todosByUserAndWeek.getOrDefault(userId, Map.of());

                    List<AchievementResponse.WeeklyRateResponse> weeklyRates =
                            buildWeeklyRates(memberWeekMap, gathering.getTotalWeeks());

                    BigDecimal overallRate = calculateOverallRate(memberTodos);

                    return AchievementResponse.MemberAchievementResponse.builder()
                            .userId(userId)
                            .nickname(user != null ? user.getNickname() : null)
                            .weeklyRates(weeklyRates)
                            .overallRate(overallRate)
                            .build();
                })
                .toList();

        List<AchievementResponse.WeeklyRateResponse> teamWeeklyRates =
                buildWeeklyRates(teamTodosByWeek, gathering.getTotalWeeks());

        BigDecimal teamOverallRate = calculateOverallRate(todos);

        return AchievementResponse.builder()
                .members(memberResponses)
                .teamWeeklyRates(teamWeeklyRates)
                .teamOverallRate(teamOverallRate)
                .build();
    }

    public AchievementRankingResponse getRanking(Long gatheringId, Long requesterId) {
        Gathering gathering = findGathering(gatheringId);
        validateActiveMember(gatheringId, requesterId);

        List<GatheringMember> activeMembers =
                gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId);

        Set<Long> activeUserIds = activeMembers.stream()
                .map(GatheringMember::getUserId)
                .collect(Collectors.toSet());

        List<Todo> todos = todoRepository.findByGatheringIdOrderByWeekNumberAscCreatedAtAsc(gatheringId).stream()
                .filter(todo -> activeUserIds.contains(todo.getUserId()))
                .toList();

        Map<Long, UserInfo> userMap = loadUsers(
                activeMembers.stream()
                        .map(GatheringMember::getUserId)
                        .distinct()
                        .toList()
        );

        Map<Long, List<Todo>> todosByUser = groupByUser(todos);

        List<MemberRateRow> memberRates = activeMembers.stream()
                .map(member -> new MemberRateRow(
                        member.getUserId(),
                        calculateOverallRate(todosByUser.getOrDefault(member.getUserId(), List.of()))
                ))
                .sorted(Comparator
                        .comparing(MemberRateRow::overallRate, Comparator.reverseOrder())
                        .thenComparing(MemberRateRow::userId))
                .toList();

        List<AchievementRankingResponse.RankingItemResponse> ranking = new ArrayList<>();
        for (int i = 0; i < memberRates.size(); i++) {
            MemberRateRow row = memberRates.get(i);
            UserInfo user = userMap.get(row.userId());

            ranking.add(AchievementRankingResponse.RankingItemResponse.builder()
                    .rank(i + 1)
                    .userId(row.userId())
                    .nickname(user != null ? user.getNickname() : null)
                    .profileImage(user != null ? user.getProfileImage() : null)
                    .overallRate(row.overallRate())
                    .build());
        }

        return AchievementRankingResponse.of(ranking);
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

    private List<AchievementResponse.WeeklyRateResponse> buildWeeklyRates(
            Map<Integer, List<Todo>> todosByWeek,
            int totalWeeks
    ) {
        List<AchievementResponse.WeeklyRateResponse> result = new ArrayList<>();

        for (int week = 1; week <= totalWeeks; week++) {
            List<Todo> weeklyTodos = todosByWeek.getOrDefault(week, List.of());

            result.add(AchievementResponse.WeeklyRateResponse.builder()
                    .week(week)
                    .rate(calculateOverallRate(weeklyTodos))
                    .build());
        }

        return result;
    }

    private BigDecimal calculateOverallRate(List<Todo> todos) {
        if (todos == null || todos.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        long totalCount = todos.size();
        long completedCount = todos.stream()
                .filter(Todo::isCompleted)
                .count();

        return BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
    }


    private Map<Long, UserInfo> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Long> distinctUserIds = userIds.stream()
                .distinct()
                .toList();

        return userClient.findByIds(distinctUserIds);
    }

    private record MemberRateRow(
            Long userId,
            BigDecimal overallRate
    ) {
    }
}
