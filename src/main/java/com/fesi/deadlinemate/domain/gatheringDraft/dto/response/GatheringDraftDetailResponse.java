package com.fesi.deadlinemate.domain.gatheringDraft.dto.response;

import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gatheringDraft.entity.GatheringDraft;
import com.fesi.deadlinemate.domain.gatheringDraft.entity.WeeklyGuideItem;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record GatheringDraftDetailResponse(
        Long draftId,
        GatheringType type,
        List<Long> categoryIds,
        String title,
        String shortDescription,
        String description,
        List<String> tags,
        String goal,
        Integer maxMembers,
        LocalDate recruitDeadline,
        LocalDate startDate,
        LocalDate endDate,
        List<WeeklyGuideItem> weeklyGuides,
        LocalDateTime updatedAt
) {
    public static GatheringDraftDetailResponse from(GatheringDraft draft) {
        return GatheringDraftDetailResponse.builder()
                .draftId(draft.getId())
                .type(draft.getType())
                .categoryIds(draft.getCategoryIds())
                .title(draft.getTitle())
                .shortDescription(draft.getShortDescription())
                .description(draft.getDescription())
                .tags(draft.getTags())
                .goal(draft.getGoal())
                .maxMembers(draft.getMaxMembers())
                .recruitDeadline(draft.getRecruitDeadline())
                .startDate(draft.getStartDate())
                .endDate(draft.getEndDate())
                .weeklyGuides(draft.getWeeklyGuides())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }
}