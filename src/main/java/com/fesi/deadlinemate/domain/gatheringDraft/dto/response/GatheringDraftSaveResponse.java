package com.fesi.deadlinemate.domain.gatheringDraft.dto.response;

import com.fesi.deadlinemate.domain.gatheringDraft.entity.GatheringDraft;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record GatheringDraftSaveResponse(
        Long draftId,
        LocalDateTime updatedAt
) {
    public static GatheringDraftSaveResponse from(GatheringDraft draft) {
        return GatheringDraftSaveResponse.builder()
                .draftId(draft.getId())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }
}