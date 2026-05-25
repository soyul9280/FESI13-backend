package com.fesi.deadlinemate.domain.todo.repository;

import com.fesi.deadlinemate.domain.todo.entity.Todo;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    Optional<Todo> findByIdAndGatheringId(Long todoId, Long gatheringId);
    List<Todo> findByGatheringIdAndUserIdOrderByWeekNumberAscCreatedAtAsc(Long gatheringId, Long userId);
    List<Todo> findByGatheringIdOrderByWeekNumberAscCreatedAtAsc(Long gatheringId);
    List<Todo> findByGatheringIdAndUserIdInOrderByWeekNumberAscCreatedAtAsc(Long gatheringId, Collection<Long> userIds);
    List<Todo> findByGatheringIdAndWeekNumberOrderByCreatedAtAsc(Long gatheringId, int weekNumber);
    List<Todo> findByGatheringIdAndUserIdAndWeekNumberOrderByCreatedAtAsc(Long gatheringId, Long userId, int weekNumber);
    long countByGatheringIdAndUserId(Long gatheringId, Long userId);
    long countByGatheringIdAndUserIdAndIsCompletedTrue(Long gatheringId, Long userId);
    long countByGatheringIdAndUserIdAndWeekNumber(Long gatheringId, Long userId, int weekNumber);
    long countByGatheringIdAndUserIdAndWeekNumberAndIsCompletedTrue(Long gatheringId, Long userId, int weekNumber);

    @Query("SELECT t.completedAt FROM Todo t WHERE t.gatheringId = :gatheringId AND t.userId = :userId AND t.completedAt IS NOT NULL")
    List<LocalDateTime> findCompletedAtsByGatheringIdAndUserId(@Param("gatheringId") Long gatheringId, @Param("userId") Long userId);
}
