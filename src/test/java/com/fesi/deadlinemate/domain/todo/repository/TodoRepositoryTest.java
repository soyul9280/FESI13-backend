package com.fesi.deadlinemate.domain.todo.repository;


import static org.assertj.core.api.Assertions.assertThat;

import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class})
class TodoRepositoryTest {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private EntityManager em;

    private Todo todo1;
    private Todo todo2;
    private Todo todo3;
    private Todo todo4;

    @BeforeEach
    void setUp() {
        todo1 = saveTodo(1L, 100L, 1, "1주차 할 일 A", false,
                LocalDateTime.of(2026, 4, 10, 10, 0));
        todo2 = saveTodo(1L, 100L, 2, "2주차 할 일 B", true,
                LocalDateTime.of(2026, 4, 10, 11, 0));
        todo3 = saveTodo(1L, 200L, 2, "2주차 할 일 C", true,
                LocalDateTime.of(2026, 4, 10, 12, 0));
        todo4 = saveTodo(1L, 100L, 2, "2주차 할 일 D", false,
                LocalDateTime.of(2026, 4, 10, 13, 0));

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findByIdAndGatheringId")
    class FindByIdAndGatheringId {

        @Test
        @DisplayName("todoId와 gatheringId로 Todo를 조회할 수 있다")
        void find_success() {
            Optional<Todo> result = todoRepository.findByIdAndGatheringId(todo1.getId(), 1L);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(todo1.getId());
            assertThat(result.get().getGatheringId()).isEqualTo(1L);
            assertThat(result.get().getUserId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("findByGatheringIdOrderByWeekNumberAscCreatedAtAsc")
    class FindByGatheringIdOrderByWeekNumberAscCreatedAtAsc {

        @Test
        @DisplayName("모임의 Todo를 week 오름차순, createdAt 오름차순으로 조회한다")
        void findAllByGatheringId_success() {
            List<Todo> result = todoRepository.findByGatheringIdOrderByWeekNumberAscCreatedAtAsc(1L);

            assertThat(result).hasSize(4);
            assertThat(result)
                    .extracting(Todo::getContent)
                    .containsExactly("1주차 할 일 A", "2주차 할 일 B", "2주차 할 일 C", "2주차 할 일 D");
        }
    }

    @Nested
    @DisplayName("findByGatheringIdAndUserIdAndWeekNumberOrderByCreatedAtAsc")
    class FindByGatheringIdAndUserIdAndWeekNumberOrderByCreatedAtAsc {

        @Test
        @DisplayName("특정 모임, 특정 사용자, 특정 주차의 Todo를 createdAt 오름차순으로 조회한다")
        void findMyWeekTodos_success() {
            List<Todo> result = todoRepository
                    .findByGatheringIdAndUserIdAndWeekNumberOrderByCreatedAtAsc(1L, 100L, 2);

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(Todo::getContent)
                    .containsExactly("2주차 할 일 B", "2주차 할 일 D");
        }
    }

    @Nested
    @DisplayName("countByGatheringIdAndUserIdAndWeekNumberAndIsCompletedTrue")
    class CountByGatheringIdAndUserIdAndWeekNumberAndIsCompletedTrue {

        @Test
        @DisplayName("특정 모임, 특정 사용자, 특정 주차의 완료된 Todo 개수를 센다")
        void countCompletedWeekTodos_success() {
            long result = todoRepository
                    .countByGatheringIdAndUserIdAndWeekNumberAndIsCompletedTrue(1L, 100L, 2);

            assertThat(result).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findCompletedAtsByGatheringIdAndUserId")
    class FindCompletedAtsByGatheringIdAndUserId {

        @Test
        @DisplayName("completedAt이 null이 아닌 Todo의 completedAt만 반환한다")
        void findCompletedAts_excludesNullCompletedAt() {
            // todo1(미완료), todo4(미완료)는 completedAt=null → 제외
            // todo2(완료)만 포함
            List<LocalDateTime> result = todoRepository
                    .findCompletedAtsByGatheringIdAndUserId(1L, 100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isNotNull();
        }

        @Test
        @DisplayName("다른 사용자의 completedAt은 반환하지 않는다")
        void findCompletedAts_excludesOtherUsers() {
            // userId=200의 todo3만 해당
            List<LocalDateTime> result = todoRepository
                    .findCompletedAtsByGatheringIdAndUserId(1L, 200L);

            assertThat(result).hasSize(1);
        }
    }

    private Todo saveTodo(
            Long gatheringId,
            Long userId,
            int weekNumber,
            String content,
            boolean isCompleted,
            LocalDateTime createdAt
    ) {
        Todo todo = Todo.builder()
                .gatheringId(gatheringId)
                .userId(userId)
                .weekNumber(weekNumber)
                .content(content)
                .isCompleted(isCompleted)
                .completedAt(isCompleted ? createdAt.plusHours(1) : null)
                .build();

        Todo saved = todoRepository.save(todo);
        setCreatedAt(saved, createdAt);
        return saved;
    }

    private void setCreatedAt(Object target, LocalDateTime createdAt) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("createdAt");
                field.setAccessible(true);
                field.set(target, createdAt);
                return;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("createdAt field not found");
    }
}