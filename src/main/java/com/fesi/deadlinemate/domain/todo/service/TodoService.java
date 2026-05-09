package com.fesi.deadlinemate.domain.todo.service;

import com.fesi.deadlinemate.domain.achievement.service.AchievementQueryService;
import com.fesi.deadlinemate.domain.achievement.service.AchievementService;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.todo.command.CreateTodoCommand;
import com.fesi.deadlinemate.domain.todo.command.UpdateTodoCommand;
import com.fesi.deadlinemate.domain.todo.event.TodoCreatedEvent;
import com.fesi.deadlinemate.domain.todo.event.TodoDeletedEvent;
import com.fesi.deadlinemate.domain.todo.event.TodoUpdatedEvent;
import com.fesi.deadlinemate.domain.todo.dto.response.CreateTodoResponse;
import com.fesi.deadlinemate.domain.todo.dto.response.MyTodoListResponse;
import com.fesi.deadlinemate.domain.todo.dto.response.TodoListResponse;
import com.fesi.deadlinemate.domain.todo.dto.response.UpdateTodoResponse;
import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final GatheringRepository gatheringRepository;
    private final GatheringMemberRepository gatheringMemberRepository;
    private final AchievementService achievementService;
    private final UserClient userClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreateTodoResponse create(CreateTodoCommand command) {
        Gathering gathering = findGathering(command.gatheringId());

        validateActiveMember(gathering.getId(), command.userId());
        validateCurrentWeek(gathering, command.week());

        Todo todo = Todo.builder()
                .gatheringId(gathering.getId())
                .userId(command.userId())
                .weekNumber(command.week())
                .content(command.content().trim())
                .isCompleted(false)
                .completedAt(null)
                .build();

        Todo saved = todoRepository.save(todo);

        achievementService.sync(saved.getGatheringId(), saved.getUserId());

        eventPublisher.publishEvent(new TodoCreatedEvent(
                saved.getId(),
                saved.getGatheringId(),
                saved.getUserId(),
                saved.getWeekNumber(),
                saved.getContent()
        ));

        return CreateTodoResponse.from(saved);
    }

    @Transactional
    public UpdateTodoResponse update(UpdateTodoCommand command) {
        Gathering gathering = findGathering(command.gatheringId());

        Todo todo = todoRepository.findByIdAndGatheringId(command.todoId(), command.gatheringId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));

        validateActiveMember(gathering.getId(), command.userId());
        todo.validateOwner(command.userId());
        validateCurrentWeek(gathering, todo.getWeekNumber());

        boolean contentChanged = false;
        boolean completedChanged = false;

        if (command.content() != null) {
            contentChanged = todo.updateContent(command.content());
        }

        if (command.isCompleted() != null) {
            completedChanged = todo.changeCompleted(command.isCompleted());
        }

        boolean changed = contentChanged || completedChanged;

        if (!changed) {
            throw new BusinessException(ErrorCode.TODO_NOT_CHANGED);
        }

        achievementService.sync(todo.getGatheringId(), todo.getUserId());

        eventPublisher.publishEvent(new TodoUpdatedEvent(
                todo.getId(),
                todo.getGatheringId(),
                todo.getUserId(),
                todo.getContent(),
                todo.isCompleted()
        ));

        return UpdateTodoResponse.from(todo);
    }

    @Transactional
    public void delete(Long gatheringId, Long todoId, Long requesterId) {
        Gathering gathering = findGathering(gatheringId);

        Todo todo = todoRepository.findByIdAndGatheringId(todoId, gatheringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));

        validateActiveMember(gathering.getId(), requesterId);
        todo.validateDeletableBy(requesterId);
        validateCurrentWeek(gathering, todo.getWeekNumber());

        Long ownerId = todo.getUserId();

        todoRepository.delete(todo);
        todoRepository.flush();

        achievementService.sync(gatheringId, ownerId);

        eventPublisher.publishEvent(new TodoDeletedEvent(
                todo.getId(),
                todo.getGatheringId(),
                todo.getUserId(),
                todo.getWeekNumber()
        ));
    }

    public TodoListResponse getTodos(Long gatheringId, Long requesterId, Integer week) {
        Gathering gathering = findGathering(gatheringId);
        validateActiveMember(gathering.getId(), requesterId);

        List<Todo> todos = (week == null)
                ? todoRepository.findByGatheringIdOrderByWeekNumberAscCreatedAtAsc(gatheringId)
                : todoRepository.findByGatheringIdAndWeekNumberOrderByCreatedAtAsc(gatheringId, week);

        Set<Long> activeUserIds = gatheringMemberRepository
                .findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId)
                .stream()
                .map(GatheringMember::getUserId)
                .collect(Collectors.toSet());

        todos = todos.stream()
                .filter(todo -> activeUserIds.contains(todo.getUserId()))
                .toList();

        Map<Long, UserInfo> userMap = loadUsers(
                todos.stream()
                        .map(Todo::getUserId)
                        .distinct()
                        .toList()
        );

        List<TodoListResponse.TodoItemResponse> responses = todos.stream()
                .map(todo -> {
                    UserInfo user = userMap.get(todo.getUserId());
                    return TodoListResponse.TodoItemResponse.builder()
                            .id(todo.getId())
                            .userId(todo.getUserId())
                            .nickname(user != null ? user.getNickname() : null)
                            .week(todo.getWeekNumber())
                            .content(todo.getContent())
                            .isCompleted(todo.isCompleted())
                            .createdAt(todo.getCreatedAt())
                            .build();
                })
                .toList();

        return TodoListResponse.of(responses);
    }

    public MyTodoListResponse getMyTodos(Long gatheringId, Long requesterId, Integer week) {
        Gathering gathering = findGathering(gatheringId);
        validateActiveMember(gathering.getId(), requesterId);

        List<Todo> todos = (week == null)
                ? todoRepository.findByGatheringIdAndUserIdOrderByWeekNumberAscCreatedAtAsc(gatheringId, requesterId)
                : todoRepository.findByGatheringIdAndUserIdAndWeekNumberOrderByCreatedAtAsc(gatheringId, requesterId, week);

        BigDecimal weeklyAchievementRate = calculateWeeklyAchievementRate(gathering, requesterId, week);
        BigDecimal overallAchievementRate = calculateOverallAchievementRate(gatheringId, requesterId);

        List<MyTodoListResponse.MyTodoItemResponse> responses = todos.stream()
                .map(todo -> MyTodoListResponse.MyTodoItemResponse.builder()
                        .id(todo.getId())
                        .week(todo.getWeekNumber())
                        .content(todo.getContent())
                        .isCompleted(todo.isCompleted())
                        .createdAt(todo.getCreatedAt())
                        .build())
                .toList();

        return MyTodoListResponse.of(responses, weeklyAchievementRate, overallAchievementRate);
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

    private void validateCurrentWeek(Gathering gathering, int requestedWeek) {
        int currentWeek = calculateCurrentWeek(gathering.getStartDate(), gathering.getEndDate());

        if (requestedWeek != currentWeek) {
            throw new BusinessException(ErrorCode.INVALID_TODO_WEEK_ACCESS);
        }
    }

    private int calculateCurrentWeek(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_TODO_PERIOD);
        }

        long days = ChronoUnit.DAYS.between(startDate, today);
        return (int) (days / 7) + 1;
    }

    private BigDecimal calculateWeeklyAchievementRate(Gathering gathering, Long userId, Integer week) {
        Integer targetWeek = resolveWeekForRead(gathering, week);

        if (targetWeek == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        long totalCount = todoRepository.countByGatheringIdAndUserIdAndWeekNumber(
                gathering.getId(), userId, targetWeek
        );
        if (totalCount == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        long completedCount = todoRepository.countByGatheringIdAndUserIdAndWeekNumberAndIsCompletedTrue(
                gathering.getId(), userId, targetWeek
        );

        return calculateRate(completedCount, totalCount);
    }

    private Integer resolveWeekForRead(Gathering gathering, Integer requestedWeek) {
        if (requestedWeek != null) {
            return requestedWeek;
        }

        LocalDate today = LocalDate.now();

        if (today.isBefore(gathering.getStartDate())) {
            return null;
        }

        if (today.isAfter(gathering.getEndDate())) {
            return gathering.getTotalWeeks();
        }

        long days = ChronoUnit.DAYS.between(gathering.getStartDate(), today);
        return (int) (days / 7) + 1;
    }

    private BigDecimal calculateOverallAchievementRate(Long gatheringId, Long userId) {
        long totalCount = todoRepository.countByGatheringIdAndUserId(gatheringId, userId);
        if (totalCount == 0) {
            return BigDecimal.ZERO.setScale(1);
        }

        long completedCount = todoRepository.countByGatheringIdAndUserIdAndIsCompletedTrue(gatheringId, userId);
        return calculateRate(completedCount, totalCount);
    }

    private BigDecimal calculateRate(long completedCount, long totalCount) {
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
}
