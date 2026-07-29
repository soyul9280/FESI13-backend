package com.fesi.deadlinemate.domain.gatheringDraft.dto.response;

import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gatheringDraft.entity.GatheringDraft;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record GatheringDraftListResponse(
        List<GatheringDraftSummary> drafts,
        int totalCount
) {
    public static GatheringDraftListResponse from(List<GatheringDraft> drafts) {
        List<GatheringDraftSummary> summaries = drafts.stream()
                .map(GatheringDraftSummary::from)
                .toList();

        return GatheringDraftListResponse.builder()
                .drafts(summaries)
                .totalCount(summaries.size())
                .build();
    }

    @Builder
    public record GatheringDraftSummary(
            Long draftId,
            String title,
            GatheringType type,
            LocalDateTime updatedAt
    ) {
        public static GatheringDraftSummary from(GatheringDraft draft) {
            return GatheringDraftSummary.builder()
                    .draftId(draft.getId())
                    .title(draft.getTitle())
                    .type(draft.getType())
                    .updatedAt(draft.getUpdatedAt())
                    .build();
        }
    }
}