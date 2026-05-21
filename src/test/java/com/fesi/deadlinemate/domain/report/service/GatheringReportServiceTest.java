package com.fesi.deadlinemate.domain.report.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.report.entity.GatheringReport;
import com.fesi.deadlinemate.domain.report.repository.GatheringReportRepository;
import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatheringReportServiceTest {

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private GatheringMemberRepository gatheringMemberRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private GatheringReportRepository gatheringReportRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GatheringReportService gatheringReportService;

    @Test
    @DisplayName("완료된 모임의 결과 리포트를 생성한다")
    void createReport_success() throws Exception {
        // given
        Long gatheringId = 1L;

        Gathering gathering = completedGathering(
                gatheringId,
                10L,
                "React 완전 정복 스터디",
                4
        );

        GatheringMember leader = activeMember(gatheringId, 100L, GatheringRole.LEADER);
        GatheringMember member = activeMember(gatheringId, 200L, GatheringRole.MEMBER);

        List<Todo> todos = List.of(
                // user 100
                todo(gatheringId, 100L, 1, true),
                todo(gatheringId, 100L, 1, true),
                todo(gatheringId, 100L, 2, true),
                todo(gatheringId, 100L, 2, true),
                todo(gatheringId, 100L, 3, true),
                todo(gatheringId, 100L, 3, false),
                todo(gatheringId, 100L, 4, true),
                todo(gatheringId, 100L, 4, false),

                // user 200
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
        when(gatheringReportRepository.findByGatheringId(gatheringId)).thenReturn(Optional.empty());
        when(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId))
                .thenReturn(List.of(leader, member));
        when(todoRepository.findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(
                eq(gatheringId), anyCollection()))
                .thenReturn(todos);
        when(objectMapper.writeValueAsString(anyList()))
                .thenReturn("[{\"week\":1,\"rate\":50.0},{\"week\":2,\"rate\":75.0},{\"week\":3,\"rate\":75.0},{\"week\":4,\"rate\":75.0}]");

        // when
        gatheringReportService.createReport(gatheringId);

        // then
        ArgumentCaptor<GatheringReport> captor = ArgumentCaptor.forClass(GatheringReport.class);
        verify(gatheringReportRepository).save(captor.capture());

        GatheringReport saved = captor.getValue();

        assertThat(saved.getGatheringId()).isEqualTo(gatheringId);
        assertThat(saved.getTeamOverallRate()).isEqualByComparingTo("56.30");
        assertThat(saved.getMvpUserIds()).containsExactly(100L);
        assertThat(saved.getLongestStreakUserIds()).containsExactly(100L);
        assertThat(saved.getLongestStreakValue()).isEqualTo(2);
        assertThat(saved.getMostImprovedUserIds()).containsExactly(200L);
        assertThat(saved.getAttendanceUserIds()).containsExactly(100L);
        assertThat(saved.getWeeklyRates()).isEqualTo(
                "[{\"week\":1,\"rate\":50.0},{\"week\":2,\"rate\":75.0},{\"week\":3,\"rate\":75.0},{\"week\":4,\"rate\":75.0}]"
        );

        verify(objectMapper, times(1)).writeValueAsString(anyList());
    }

    @Test
    @DisplayName("모임이 완료 상태가 아니면 리포트를 생성할 수 없다")
    void createReport_fail_whenGatheringNotCompleted() {
        // given
        Long gatheringId = 1L;

        Gathering gathering = Gathering.builder()
                .leaderId(10L)
                .type(GatheringType.STUDY)
                .title("진행중 모임")
                .shortDescription("소개")
                .description("설명")
                .goal("목표")
                .maxMembers(6)
                .currentMembers(3)
                .recruitDeadline(LocalDate.of(2025, 3, 1))
                .startDate(LocalDate.of(2025, 3, 2))
                .endDate(LocalDate.of(2025, 3, 30))
                .totalWeeks(5)
                .status(GatheringStatus.IN_PROGRESS)
                .viewCount(0)
                .build();

        setField(gathering, "id", gatheringId);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));

        // when & then
        assertThatThrownBy(() -> gatheringReportService.createReport(gatheringId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.GATHERING_REPORT_NOT_AVAILABLE);

        verify(gatheringReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 결과 리포트가 존재하면 생성할 수 없다")
    void createReport_fail_whenReportAlreadyExists() {
        // given
        Long gatheringId = 1L;

        Gathering gathering = completedGathering(gatheringId, 10L, "완료 모임", 4);

        GatheringReport existing = GatheringReport.builder()
                .gatheringId(gatheringId)
                .teamOverallRate(new BigDecimal("80.00"))
                .mvpUserIds(List.of(100L))
                .longestStreakUserIds(List.of(100L))
                .mostImprovedUserIds(List.of(200L))
                .attendanceUserIds(List.of(100L))
                .weeklyRates("[]")
                .build();

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(gatheringReportRepository.findByGatheringId(gatheringId)).thenReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> gatheringReportService.createReport(gatheringId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.GATHERING_REPORT_ALREADY_EXISTS);

        verify(gatheringReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("아무도 완벽한 주차가 없으면 longestStreakUserIds가 비어야 한다")
    void createReport_whenNoStreakExists_longestStreakIsEmpty() throws Exception {
        // given
        Long gatheringId = 1L;
        Gathering gathering = completedGathering(gatheringId, 10L, "스트릭 없는 모임", 2);
        GatheringMember leader = activeMember(gatheringId, 100L, GatheringRole.LEADER);
        GatheringMember member = activeMember(gatheringId, 200L, GatheringRole.MEMBER);

        List<Todo> todos = List.of(
                // user 100 - week1: 50%, week2: 50%
                todo(gatheringId, 100L, 1, true),
                todo(gatheringId, 100L, 1, false),
                todo(gatheringId, 100L, 2, true),
                todo(gatheringId, 100L, 2, false),
                // user 200 - week1: 50%, week2: 50%
                todo(gatheringId, 200L, 1, true),
                todo(gatheringId, 200L, 1, false),
                todo(gatheringId, 200L, 2, true),
                todo(gatheringId, 200L, 2, false)
        );

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(gatheringReportRepository.findByGatheringId(gatheringId)).thenReturn(Optional.empty());
        when(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId))
                .thenReturn(List.of(leader, member));
        when(todoRepository.findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(
                eq(gatheringId), anyCollection()))
                .thenReturn(todos);
        when(objectMapper.writeValueAsString(anyList())).thenReturn("[]");

        // when
        gatheringReportService.createReport(gatheringId);

        // then
        ArgumentCaptor<GatheringReport> captor = ArgumentCaptor.forClass(GatheringReport.class);
        verify(gatheringReportRepository).save(captor.capture());
        assertThat(captor.getValue().getLongestStreakUserIds()).isEmpty();
    }

    @Test
    @DisplayName("모든 멤버의 달성률이 하락하면 mostImprovedUserIds가 비어야 한다")
    void createReport_whenAllImprovementNegative_mostImprovedIsEmpty() throws Exception {
        // given
        Long gatheringId = 1L;
        Gathering gathering = completedGathering(gatheringId, 10L, "하락 모임", 2);
        GatheringMember leader = activeMember(gatheringId, 100L, GatheringRole.LEADER);
        GatheringMember member = activeMember(gatheringId, 200L, GatheringRole.MEMBER);

        List<Todo> todos = List.of(
                // user 100 - week1: 100%, week2: 50% → improvement = -50
                todo(gatheringId, 100L, 1, true),
                todo(gatheringId, 100L, 1, true),
                todo(gatheringId, 100L, 2, true),
                todo(gatheringId, 100L, 2, false),
                // user 200 - week1: 100%, week2: 50% → improvement = -50
                todo(gatheringId, 200L, 1, true),
                todo(gatheringId, 200L, 1, true),
                todo(gatheringId, 200L, 2, true),
                todo(gatheringId, 200L, 2, false)
        );

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(gatheringReportRepository.findByGatheringId(gatheringId)).thenReturn(Optional.empty());
        when(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId))
                .thenReturn(List.of(leader, member));
        when(todoRepository.findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(
                eq(gatheringId), anyCollection()))
                .thenReturn(todos);
        when(objectMapper.writeValueAsString(anyList())).thenReturn("[]");

        // when
        gatheringReportService.createReport(gatheringId);

        // then
        ArgumentCaptor<GatheringReport> captor = ArgumentCaptor.forClass(GatheringReport.class);
        verify(gatheringReportRepository).save(captor.capture());
        assertThat(captor.getValue().getMostImprovedUserIds()).isEmpty();
    }

    @Test
    @DisplayName("주간 달성률 JSON 직렬화에 실패하면 예외가 발생한다")
    void createReport_fail_whenWeeklyRatesSerializationFails() throws Exception {
        // given
        Long gatheringId = 1L;

        Gathering gathering = completedGathering(gatheringId, 10L, "완료 모임", 2);
        GatheringMember leader = activeMember(gatheringId, 100L, GatheringRole.LEADER);

        List<Todo> todos = List.of(
                todo(gatheringId, 100L, 1, true),
                todo(gatheringId, 100L, 2, true)
        );

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(gatheringReportRepository.findByGatheringId(gatheringId)).thenReturn(Optional.empty());
        when(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId))
                .thenReturn(List.of(leader));
        when(todoRepository.findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(
                eq(gatheringId), anyCollection()))
                .thenReturn(todos);
        when(objectMapper.writeValueAsString(anyList()))
                .thenThrow(new JsonProcessingException("serialize fail") {});

        // when & then
        assertThatThrownBy(() -> gatheringReportService.createReport(gatheringId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_GATHERING_REPORT_DATA);

        verify(gatheringReportRepository, never()).save(any());
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
                .recruitDeadline(LocalDate.of(2025, 3, 1))
                .startDate(LocalDate.of(2025, 3, 2))
                .endDate(LocalDate.of(2025, 3, 30))
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