package com.fesi.deadlinemate.domain.report.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.report.dto.GatheringReportResponse;
import com.fesi.deadlinemate.domain.report.entity.GatheringReport;
import com.fesi.deadlinemate.domain.report.repository.GatheringReportRepository;
import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GatheringReportQueryServiceTest {

    @Mock
    private GatheringRepository gatheringRepository;
    @Mock
    private GatheringMemberRepository gatheringMemberRepository;
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private GatheringReportRepository gatheringReportRepository;
    @Mock
    private UserClient userClient;

    private GatheringReportQueryService gatheringReportQueryService;

    @BeforeEach
    void setUp() {
        gatheringReportQueryService = new GatheringReportQueryService(
                gatheringRepository,
                gatheringMemberRepository,
                todoRepository,
                gatheringReportRepository,
                userClient,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("완료된 모임의 결과 리포트를 조회한다")
    void getReport_success() {
        // given
        Long gatheringId = 1L;
        Long requesterId = 100L;

        Gathering gathering = completedGathering(gatheringId, 10L, "React 완전 정복 스터디", 4);

        GatheringReport report = GatheringReport.builder()
                .gatheringId(gatheringId)
                .teamOverallRate(new BigDecimal("78.50"))
                .mvpUserIds(List.of(100L))
                .longestStreakUserIds(List.of(100L))
                .longestStreakValue(2)
                .mostImprovedUserIds(List.of(200L))
                .attendanceUserIds(List.of(100L))
                .weeklyRates("""
                        [
                          {"week":1,"rate":90.0},
                          {"week":2,"rate":80.0},
                          {"week":3,"rate":75.0},
                          {"week":4,"rate":68.5}
                        ]
                        """)
                .build();

        GatheringMember leader = activeMember(gatheringId, 100L, GatheringRole.LEADER);
        GatheringMember member = activeMember(gatheringId, 200L, GatheringRole.MEMBER);

        List<Todo> todos = List.of(
                // user 100: weekly [100, 100, 50, 50], overall 75.0, longestStreak 2
                todo(gatheringId, 100L, 1, true),
                todo(gatheringId, 100L, 1, true),
                todo(gatheringId, 100L, 2, true),
                todo(gatheringId, 100L, 2, true),
                todo(gatheringId, 100L, 3, true),
                todo(gatheringId, 100L, 3, false),
                todo(gatheringId, 100L, 4, true),
                todo(gatheringId, 100L, 4, false),

                // user 200: weekly [0, 50, 50, 50], overall 37.5, longestStreak 0
                todo(gatheringId, 200L, 1, false),
                todo(gatheringId, 200L, 1, false),
                todo(gatheringId, 200L, 2, true),
                todo(gatheringId, 200L, 2, false),
                todo(gatheringId, 200L, 3, true),
                todo(gatheringId, 200L, 3, false),
                todo(gatheringId, 200L, 4, true),
                todo(gatheringId, 200L, 4, false)
        );

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(gatheringId, requesterId))
                .thenReturn(true);
        when(gatheringReportRepository.findByGatheringId(gatheringId)).thenReturn(Optional.of(report));
        when(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId))
                .thenReturn(List.of(leader, member));
        when(todoRepository.findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(
                eq(gatheringId), anyCollection()))
                .thenReturn(todos);

        when(userClient.findById(100L)).thenReturn(userInfo(100L, "마감왕", "leader.png"));
        when(userClient.findById(200L)).thenReturn(userInfo(200L, "성장맨", "member.png"));

        // when
        GatheringReportResponse response = gatheringReportQueryService.getReport(gatheringId, requesterId);

        // then
        assertThat(response.gathering().title()).isEqualTo("React 완전 정복 스터디");
        assertThat(response.gathering().startDate()).isEqualTo(LocalDate.of(2025, 3, 22));
        assertThat(response.gathering().endDate()).isEqualTo(LocalDate.of(2025, 4, 19));

        assertThat(response.teamOverallRate()).isEqualByComparingTo("78.5");
        assertThat(response.weeklyRates()).hasSize(4);
        assertThat(response.weeklyRates().get(0).week()).isEqualTo(1);
        assertThat(response.weeklyRates().get(0).rate()).isEqualByComparingTo("90.0");

        assertThat(response.memberResults()).hasSize(2);

        GatheringReportResponse.MemberResultResponse leaderResult = response.memberResults().stream()
                .filter(it -> it.userId().equals(100L))
                .findFirst()
                .orElseThrow();

        assertThat(leaderResult.nickname()).isEqualTo("마감왕");
        assertThat(leaderResult.overallRate()).isEqualByComparingTo("75.0");
        assertThat(leaderResult.longestStreak()).isEqualTo(2);
        assertThat(leaderResult.completedTodos()).isEqualTo(6);
        assertThat(leaderResult.totalTodos()).isEqualTo(8);
        assertThat(leaderResult.weeklyRates()).containsExactly(
                new BigDecimal("100.0"),
                new BigDecimal("100.0"),
                new BigDecimal("50.0"),
                new BigDecimal("50.0")
        );

        GatheringReportResponse.MemberResultResponse memberResult = response.memberResults().stream()
                .filter(it -> it.userId().equals(200L))
                .findFirst()
                .orElseThrow();

        assertThat(memberResult.nickname()).isEqualTo("성장맨");
        assertThat(memberResult.overallRate()).isEqualByComparingTo("37.5");
        assertThat(memberResult.longestStreak()).isEqualTo(0);
        assertThat(memberResult.completedTodos()).isEqualTo(3);
        assertThat(memberResult.totalTodos()).isEqualTo(8);

        assertThat(response.awards().mvp()).hasSize(1);
        assertThat(response.awards().mvp().get(0).userId()).isEqualTo(100L);
        assertThat(response.awards().mvp().get(0).nickname()).isEqualTo("마감왕");

        assertThat(response.awards().longestStreak()).hasSize(1);
        assertThat(response.awards().longestStreak().get(0).userId()).isEqualTo(100L);
        assertThat(response.awards().longestStreak().get(0).nickname()).isEqualTo("마감왕");
        assertThat(response.awards().longestStreak().get(0).streak()).isEqualTo(2);

        assertThat(response.awards().mostImproved()).hasSize(1);
        assertThat(response.awards().mostImproved().get(0).userId()).isEqualTo(200L);
        assertThat(response.awards().mostImproved().get(0).nickname()).isEqualTo("성장맨");

        assertThat(response.awards().attendance()).hasSize(1);
        assertThat(response.awards().attendance().get(0).userId()).isEqualTo(100L);
        assertThat(response.awards().attendance().get(0).nickname()).isEqualTo("마감왕");
    }

    @Test
    @DisplayName("활성 멤버가 아니면 결과 리포트를 조회할 수 없다")
    void getReport_fail_whenRequesterIsNotActiveMember() {
        // given
        Long gatheringId = 1L;
        Long requesterId = 999L;

        Gathering gathering = completedGathering(gatheringId, 10L, "완료된 모임", 4);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(gatheringId, requesterId))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> gatheringReportQueryService.getReport(gatheringId, requesterId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_A_MEMBER);
    }

    @Test
    @DisplayName("모임이 완료 상태가 아니면 결과 리포트를 조회할 수 없다")
    void getReport_fail_whenGatheringIsNotCompleted() {
        // given
        Long gatheringId = 1L;
        Long requesterId = 100L;

        Gathering gathering = Gathering.builder()
                .leaderId(10L)
                .type(GatheringType.STUDY)
                .title("진행중 모임")
                .shortDescription("소개")
                .description("설명")
                .goal("목표")
                .maxMembers(6)
                .currentMembers(3)
                .recruitDeadline(LocalDate.of(2025, 3, 20))
                .startDate(LocalDate.of(2025, 3, 22))
                .endDate(LocalDate.of(2025, 4, 19))
                .totalWeeks(4)
                .status(GatheringStatus.IN_PROGRESS)
                .viewCount(0)
                .build();

        setField(gathering, "id", gatheringId);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(gatheringId, requesterId))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> gatheringReportQueryService.getReport(gatheringId, requesterId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.GATHERING_REPORT_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("주간 달성률 JSON 형식이 올바르지 않으면 예외가 발생한다")
    void getReport_fail_whenWeeklyRatesJsonInvalid() {
        // given
        Long gatheringId = 1L;
        Long requesterId = 100L;

        Gathering gathering = completedGathering(gatheringId, 10L, "완료된 모임", 2);

        GatheringReport report = GatheringReport.builder()
                .gatheringId(gatheringId)
                .teamOverallRate(new BigDecimal("80.00"))
                .mvpUserIds(List.of(100L))
                .longestStreakUserIds(List.of(100L))
                .longestStreakValue(0)
                .mostImprovedUserIds(List.of(100L))
                .attendanceUserIds(List.of(100L))
                .weeklyRates("invalid-json")
                .build();

        GatheringMember leader = activeMember(gatheringId, 100L, GatheringRole.LEADER);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(gatheringId, requesterId))
                .thenReturn(true);
        when(gatheringReportRepository.findByGatheringId(gatheringId)).thenReturn(Optional.of(report));
        when(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId))
                .thenReturn(List.of(leader));
        when(todoRepository.findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(
                eq(gatheringId), anyCollection()))
                .thenReturn(List.of(todo(gatheringId, 100L, 1, true)));
        when(userClient.findById(100L)).thenReturn(userInfo(100L, "마감왕", "leader.png"));

        // when & then
        assertThatThrownBy(() -> gatheringReportQueryService.getReport(gatheringId, requesterId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_GATHERING_REPORT_DATA);
    }

    private Gathering completedGathering(Long id, Long leaderId, String title, int totalWeeks) {
        Gathering gathering = Gathering.builder()
                .leaderId(leaderId)
                .type(GatheringType.STUDY)
                .title(title)
                .shortDescription("한 줄 소개")
                .description("상세 설명")
                .goal("목표")
                .maxMembers(6)
                .currentMembers(2)
                .recruitDeadline(LocalDate.of(2025, 3, 20))
                .startDate(LocalDate.of(2025, 3, 22))
                .endDate(LocalDate.of(2025, 4, 19))
                .totalWeeks(totalWeeks)
                .status(GatheringStatus.COMPLETED)
                .viewCount(0)
                .build();

        setField(gathering, "id", id);
        return gathering;
    }

    private GatheringMember activeMember(Long gatheringId, Long userId, GatheringRole role) {
        GatheringMember member = GatheringMember.builder()
                .gatheringId(gatheringId)
                .userId(userId)
                .role(role)
                .personalGoal("개인 목표")
                .isActive(true)
                .build();

        setField(member, "id", userId);
        return member;
    }

    private Todo todo(Long gatheringId, Long userId, int weekNumber, boolean completed) {
        Todo todo = Todo.builder()
                .gatheringId(gatheringId)
                .userId(userId)
                .weekNumber(weekNumber)
                .content("todo-" + userId + "-" + weekNumber)
                .isCompleted(completed)
                .completedAt(completed ? LocalDateTime.now() : null)
                .build();

        setField(todo, "id", System.nanoTime());
        return todo;
    }

    private UserInfo userInfo(Long id, String nickname, String profileImage) {
        return UserInfo.builder()
                .id(id)
                .nickname(nickname)
                .profileImage(profileImage)
                .reputationScore(new BigDecimal("36.5"))
                .build();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}