package com.fesi.deadlinemate.domain.gatheringDraft.entity;

import java.util.List;

public record WeeklyGuideItem(
        Integer week,
        String title,
        List<String> details
) {
}
