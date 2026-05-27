package com.fesi.deadlinemate.domain.todo.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fesi.deadlinemate.domain.achievement.service.AchievementService;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.todo.command.CreateTodoCommand;
import com.fesi.deadlinemate.domain.todo.command.UpdateTodoCommand;
import com.fesi.deadlinemate.domain.todo.dto.response.CreateTodoResponse;
import com.fesi.deadlinemate.domain.todo.dto.response.MyTodoListResponse;
import com.fesi.deadlinemate.domain.todo.dto.response.TodoListResponse;
import com.fesi.deadlinemate.domain.todo.dto.response.UpdateTodoResponse;
import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.domain.todo.event.TodoCreatedEvent;
import com.fesi.deadlinemate.domain.todo.event.TodoDeletedEvent;
import com.fesi.deadlinemate.domain.todo.event.TodoUpdatedEvent;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private GatheringMemberRepository gatheringMemberRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AchievementService achievementService;

    @Mock
    private Clock clock;

    @InjectMocks
    private TodoService todoService;

    private static final LocalDate TEST_TODAY = LocalDate.of(2026, 5, 27);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TEST_TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    private Gathering gathering;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();

        gathering = Gathering.builder()
                .leaderId(10L)
                .type(GatheringType.STUDY)
                .title("Spring Study")
                .shortDescription("스프링 스터디")
                .description("설명")
                .goal("목표")
                .maxMembers(5)
                .currentMembers(3)
                .recruitDeadline(today.minusDays(10))
                .startDate(today.minusDays(2))   // 현재 1주차 진행 중
                .endDate(today.plusDays(20))
                .totalWeeks(4)
                .status(GatheringStatus.IN_PROGRESS)
                .viewCount(0)
                .build();

        setField(gathering, "id", 100L);

        lenient().when(clock.instant()).thenReturn(FIXED_CLOCK.instant());
        lenient().when(clock.getZone()).thenReturn(FIXED_CLOCK.getZone());
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("정상 생성이면 Todo를 저장하고 이벤트를 발행한다")
        void create_success() {
            CreateTodoCommand command = CreateTodoCommand.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .week(1)
                    .content("  스프링 1장 읽기  ")
                    .build();

            Todo savedTodo = Todo.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .weekNumber(1)
                    .content("스프링 1장 읽기")
                    .isCompleted(false)
                    .completedAt(null)
                    .build();
            setField(savedTodo, "id", 1L);
            setField(savedTodo, "createdAt", LocalDateTime.of(2025, 3, 22, 9, 0));

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);
            when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

            CreateTodoResponse response = todoService.create(command);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.week()).isEqualTo(1);
            assertThat(response.content()).isEqualTo("스프링 1장 읽기");
            assertThat(response.isCompleted()).isFalse();

            ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
            verify(todoRepository).save(todoCaptor.capture());

            Todo captured = todoCaptor.getValue();
            assertThat(captured.getGatheringId()).isEqualTo(100L);
            assertThat(captured.getUserId()).isEqualTo(200L);
            assertThat(captured.getWeekNumber()).isEqualTo(1);
            assertThat(captured.getContent()).isEqualTo("스프링 1장 읽기");
            assertThat(captured.isCompleted()).isFalse();

            ArgumentCaptor<TodoCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TodoCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            TodoCreatedEvent event = eventCaptor.getValue();
            assertThat(event.todoId()).isEqualTo(1L);
            assertThat(event.gatheringId()).isEqualTo(100L);
            assertThat(event.userId()).isEqualTo(200L);
            assertThat(event.weekNumber()).isEqualTo(1);
            assertThat(event.content()).isEqualTo("스프링 1장 읽기");
        }

        @Test
        @DisplayName("활성 멤버가 아니면 생성할 수 없다")
        void create_fail_notActiveMember() {
            CreateTodoCommand command = CreateTodoCommand.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .week(1)
                    .content("할 일")
                    .build();

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(false);

            assertThatThrownBy(() -> todoService.create(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_A_MEMBER);

            verify(todoRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("현재 주차가 아니면 생성할 수 없다")
        void create_fail_invalidWeek() {
            CreateTodoCommand command = CreateTodoCommand.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .week(2)
                    .content("할 일")
                    .build();

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);

            assertThatThrownBy(() -> todoService.create(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TODO_WEEK_ACCESS);

            verify(todoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("완료 여부를 변경하면 수정되고 이벤트를 발행한다")
        void update_success_changeCompleted() {
            UpdateTodoCommand command = UpdateTodoCommand.builder()
                    .gatheringId(100L)
                    .todoId(1L)
                    .userId(200L)
                    .content(null)
                    .isCompleted(true)
                    .build();

            Todo todo = Todo.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .weekNumber(1)
                    .content("기존 할 일")
                    .isCompleted(false)
                    .completedAt(null)
                    .build();
            setField(todo, "id", 1L);
            setField(todo, "createdAt", LocalDateTime.of(2025, 3, 22, 10, 0));

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(todoRepository.findByIdAndGatheringId(1L, 100L)).thenReturn(Optional.of(todo));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);

            UpdateTodoResponse response = todoService.update(command);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.isCompleted()).isTrue();

            ArgumentCaptor<TodoUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(TodoUpdatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            TodoUpdatedEvent event = eventCaptor.getValue();
            assertThat(event.todoId()).isEqualTo(1L);
            assertThat(event.gatheringId()).isEqualTo(100L);
            assertThat(event.userId()).isEqualTo(200L);
            assertThat(event.content()).isEqualTo(todo.getContent());
            assertThat(event.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("변경 사항이 없으면 예외가 발생한다")
        void update_fail_notChanged() {
            UpdateTodoCommand command = UpdateTodoCommand.builder()
                    .gatheringId(100L)
                    .todoId(1L)
                    .userId(200L)
                    .content(null)
                    .isCompleted(null)
                    .build();

            Todo todo = Todo.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .weekNumber(1)
                    .content("기존 할 일")
                    .isCompleted(false)
                    .completedAt(null)
                    .build();
            setField(todo, "id", 1L);

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(todoRepository.findByIdAndGatheringId(1L, 100L)).thenReturn(Optional.of(todo));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);

            assertThatThrownBy(() -> todoService.update(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TODO_NOT_CHANGED);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("정상 삭제이면 Todo를 삭제하고 이벤트를 발행한다")
        void delete_success() {
            Todo todo = Todo.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .weekNumber(1)
                    .content("삭제할 할 일")
                    .isCompleted(false)
                    .completedAt(null)
                    .build();
            setField(todo, "id", 1L);

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(todoRepository.findByIdAndGatheringId(1L, 100L)).thenReturn(Optional.of(todo));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);

            todoService.delete(100L, 1L, 200L);

            verify(todoRepository).delete(todo);
            verify(todoRepository).flush();
            verify(achievementService).sync(100L, 200L);

            ArgumentCaptor<TodoDeletedEvent> eventCaptor = ArgumentCaptor.forClass(TodoDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            TodoDeletedEvent event = eventCaptor.getValue();
            assertThat(event.todoId()).isEqualTo(1L);
            assertThat(event.gatheringId()).isEqualTo(100L);
            assertThat(event.userId()).isEqualTo(200L);
            assertThat(event.weekNumber()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getTodos")
    class GetTodos {

        @Test
        @DisplayName("전체 Todo 목록을 사용자 정보와 함께 조회한다")
        void getTodos_success() {
            Todo todo1 = Todo.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .weekNumber(1)
                    .content("할 일 1")
                    .isCompleted(false)
                    .completedAt(null)
                    .build();
            setField(todo1, "id", 1L);
            setField(todo1, "createdAt", LocalDateTime.of(2025, 3, 22, 9, 0));

            Todo todo2 = Todo.builder()
                    .gatheringId(100L)
                    .userId(201L)
                    .weekNumber(1)
                    .content("할 일 2")
                    .isCompleted(true)
                    .completedAt(LocalDateTime.of(2025, 3, 22, 12, 0))
                    .build();
            setField(todo2, "id", 2L);
            setField(todo2, "createdAt", LocalDateTime.of(2025, 3, 22, 10, 0));

            GatheringMember member1 = GatheringMember.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .role(GatheringRole.LEADER)
                    .personalGoal(null)
                    .overallAchievementRate(BigDecimal.ZERO.setScale(1))
                    .isActive(true)
                    .build();

            GatheringMember member2 = GatheringMember.builder()
                    .gatheringId(100L)
                    .userId(201L)
                    .role(GatheringRole.MEMBER)
                    .personalGoal(null)
                    .overallAchievementRate(BigDecimal.ZERO.setScale(1))
                    .isActive(true)
                    .build();

            UserInfo user1 = UserInfo.builder()
                    .id(200L)
                    .nickname("유저1")
                    .profileImage("profile1.png")
                    .reputationScore(BigDecimal.valueOf(36.5))
                    .build();

            UserInfo user2 = UserInfo.builder()
                    .id(201L)
                    .nickname("유저2")
                    .profileImage("profile2.png")
                    .reputationScore(BigDecimal.valueOf(40.0))
                    .build();

            Map<Long, UserInfo> userMap = Map.of(
                    200L, user1,
                    201L, user2
            );

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);
            when(todoRepository.findByGatheringIdOrderByWeekNumberAscCreatedAtAsc(100L))
                    .thenReturn(List.of(todo1, todo2));
            when(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(100L))
                    .thenReturn(List.of(member1, member2));
            when(userClient.findByIds(List.of(200L, 201L))).thenReturn(userMap);

            TodoListResponse response = todoService.getTodos(100L, 200L, null);

            assertThat(response.todos()).hasSize(2);
            assertThat(response.todos().get(0).id()).isEqualTo(1L);
            assertThat(response.todos().get(0).nickname()).isEqualTo("유저1");
            assertThat(response.todos().get(1).id()).isEqualTo(2L);
            assertThat(response.todos().get(1).nickname()).isEqualTo("유저2");
        }
    }

    @Nested
    @DisplayName("getMyTodos")
    class GetMyTodos {

        @Test
        @DisplayName("내 Todo 목록과 주간/전체 달성률을 조회한다")
        void getMyTodos_success() {
            Todo todo1 = Todo.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .weekNumber(1)
                    .content("할 일 1")
                    .isCompleted(true)
                    .completedAt(LocalDateTime.of(2025, 3, 22, 12, 0))
                    .build();
            setField(todo1, "id", 1L);
            setField(todo1, "createdAt", LocalDateTime.of(2025, 3, 22, 9, 0));

            Todo todo2 = Todo.builder()
                    .gatheringId(100L)
                    .userId(200L)
                    .weekNumber(1)
                    .content("할 일 2")
                    .isCompleted(false)
                    .completedAt(null)
                    .build();
            setField(todo2, "id", 2L);
            setField(todo2, "createdAt", LocalDateTime.of(2025, 3, 22, 10, 0));

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);
            when(todoRepository.findByGatheringIdAndUserIdOrderByWeekNumberAscCreatedAtAsc(100L, 200L))
                    .thenReturn(List.of(todo1, todo2));

            when(todoRepository.countByGatheringIdAndUserIdAndWeekNumber(100L, 200L, 1)).thenReturn(2L);
            when(todoRepository.countByGatheringIdAndUserIdAndWeekNumberAndIsCompletedTrue(100L, 200L, 1)).thenReturn(1L);
            when(todoRepository.countByGatheringIdAndUserId(100L, 200L)).thenReturn(2L);
            when(todoRepository.countByGatheringIdAndUserIdAndIsCompletedTrue(100L, 200L)).thenReturn(1L);

            MyTodoListResponse response = todoService.getMyTodos(100L, 200L, null);

            assertThat(response.todos()).hasSize(2);
            assertThat(response.weeklyAchievementRate()).isEqualByComparingTo("50.0");
            assertThat(response.overallAchievementRate()).isEqualByComparingTo("50.0");
            assertThat(response.streakDays()).isEqualTo(0);
        }

        @Test
        @DisplayName("오늘 포함 3일 연속 완료하면 streakDays가 3이다")
        void getMyTodos_streak_threeConsecutiveDays() {
            when(clock.instant()).thenReturn(FIXED_CLOCK.instant());
            when(clock.getZone()).thenReturn(FIXED_CLOCK.getZone());

            List<LocalDateTime> completedAts = List.of(
                    TEST_TODAY.atTime(10, 0),
                    TEST_TODAY.minusDays(1).atTime(20, 0),
                    TEST_TODAY.minusDays(2).atTime(15, 0)
            );

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);
            when(todoRepository.findByGatheringIdAndUserIdOrderByWeekNumberAscCreatedAtAsc(100L, 200L))
                    .thenReturn(List.of());
            when(todoRepository.findCompletedAtsByGatheringIdAndUserId(100L, 200L)).thenReturn(completedAts);

            MyTodoListResponse response = todoService.getMyTodos(100L, 200L, null);

            assertThat(response.streakDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("가장 최근 완료일이 그저께이면 streakDays가 0이다")
        void getMyTodos_streak_brokenStreak_returnsZero() {
            when(clock.instant()).thenReturn(FIXED_CLOCK.instant());
            when(clock.getZone()).thenReturn(FIXED_CLOCK.getZone());

            List<LocalDateTime> completedAts = List.of(
                    TEST_TODAY.minusDays(2).atTime(10, 0),
                    TEST_TODAY.minusDays(3).atTime(10, 0)
            );

            when(gatheringRepository.findById(100L)).thenReturn(Optional.of(gathering));
            when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(100L, 200L)).thenReturn(true);
            when(todoRepository.findByGatheringIdAndUserIdOrderByWeekNumberAscCreatedAtAsc(100L, 200L))
                    .thenReturn(List.of());
            when(todoRepository.findCompletedAtsByGatheringIdAndUserId(100L, 200L)).thenReturn(completedAts);

            MyTodoListResponse response = todoService.getMyTodos(100L, 200L, null);

            assertThat(response.streakDays()).isEqualTo(0);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}