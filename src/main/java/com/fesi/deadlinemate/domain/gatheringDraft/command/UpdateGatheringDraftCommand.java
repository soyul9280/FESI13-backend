package com.fesi.deadlinemate.domain.gatheringDraft.command;

import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gatheringDraft.entity.WeeklyGuideItem;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record UpdateGatheringDraftCommand(
        Long userId,
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
        List<WeeklyGuideItem> weeklyGuides
) {
}