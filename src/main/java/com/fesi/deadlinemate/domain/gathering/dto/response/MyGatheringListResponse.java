package com.fesi.deadlinemate.domain.gathering.dto.response;

import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record MyGatheringListResponse(
        List<MyGatheringItem> gatherings,
        long totalCount,
        int totalPages,
        int currentPage
) {
    @Builder
    public record MyGatheringItem(
            Long id,
            String type,
            List<String> categories,
            String title,
            String shortDescription,
            List<String> tags,
            int maxMembers,
            int currentMembers,
            LocalDate startDate,
            LocalDate endDate,
            GatheringStatus status,
            GatheringRole myRole,
            boolean isLiked,
            boolean hasReviewed,
            int reviewedMembersCount,
            @Nullable Integer pendingApplicationCount
    ) {
        public static MyGatheringItem of(Gathering gathering, GatheringRole myRole,
                                         List<String> categories, List<String> tags,
                                         boolean hasReviewed, int reviewedMembersCount,
                                         Integer pendingApplicationCount) {
            return MyGatheringItem.builder()
                    .id(gathering.getId())
                    .type(gathering.getType().getDisplayName())
                    .categories(categories)
                    .title(gathering.getTitle())
                    .shortDescription(gathering.getShortDescription())
                    .tags(tags)
                    .maxMembers(gathering.getMaxMembers())
                    .currentMembers(gathering.getCurrentMembers())
                    .startDate(gathering.getStartDate())
                    .endDate(gathering.getEndDate())
                    .status(gathering.getStatus())
                    .myRole(myRole)
                    .isLiked(false)
                    .hasReviewed(hasReviewed)
                    .reviewedMembersCount(reviewedMembersCount)
                    .pendingApplicationCount(pendingApplicationCount)
                    .build();
        }
    }
}