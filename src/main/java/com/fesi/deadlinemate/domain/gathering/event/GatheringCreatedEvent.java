package com.fesi.deadlinemate.domain.gathering.event;

public record GatheringCreatedEvent(
        Long gatheringId,
        Long leaderId,
        String title,
        Long draftId
) {
}
