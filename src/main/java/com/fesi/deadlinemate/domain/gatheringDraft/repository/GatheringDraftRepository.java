package com.fesi.deadlinemate.domain.gatheringDraft.repository;

import com.fesi.deadlinemate.domain.gatheringDraft.entity.GatheringDraft;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatheringDraftRepository extends JpaRepository<GatheringDraft, Long> {
    long countByUserId(Long userId);

    List<GatheringDraft> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
