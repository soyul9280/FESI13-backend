package com.fesi.deadlinemate.domain.gatheringDraft.repository;

import com.fesi.deadlinemate.domain.gatheringDraft.entity.GatheringDraft;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringDraftRepository extends JpaRepository<GatheringDraft, Long> {
    List<GatheringDraft> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from GatheringDraft d where d.userId = :userId")
    List<GatheringDraft> findByUserIdForUpdate(@Param("userId") Long userId);
}
