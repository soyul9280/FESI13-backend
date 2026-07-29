package com.fesi.deadlinemate.domain.gatheringDraft.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fesi.deadlinemate.domain.gatheringDraft.command.CreateGatheringDraftCommand;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gatheringDraft.entity.WeeklyGuideItem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateGatheringDraftRequest(
        @Schema(example = "STUDY", description = "모임 유형 (STUDY | PROJECT)")
        GatheringType type,

        @ArraySchema(schema = @Schema(type = "integer", format = "int64", example = "1"))
        List<Long> categoryIds,

        @Schema(example = "React 완전 정복 스터디")
        @Size(max = 60, message = "제목은 60자 이하여야 합니다.")
        String title,

        @Schema(example = "리액트 공식문서를 같이 읽어요")
        @Size(max = 100, message = "한 줄 소개는 100자 이하여야 합니다.")
        String shortDescription,

        String description,

        @ArraySchema(schema = @Schema(example = "React"))
        List<String> tags,

        String goal,

        @Schema(example = "6")
        Integer maxMembers,

        @Schema(example = "2026-05-15", type = "string", format = "date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate recruitDeadline,

        @Schema(example = "2026-05-20", type = "string", format = "date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @Schema(example = "2026-08-20", type = "string", format = "date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        List<WeeklyGuideItem> weeklyGuides
) {
    public CreateGatheringDraftCommand toCommand(Long userId) {
        return CreateGatheringDraftCommand.builder()
                .userId(userId)
                .type(type)
                .categoryIds(categoryIds)
                .title(title)
                .shortDescription(shortDescription)
                .description(description)
                .tags(tags)
                .goal(goal)
                .maxMembers(maxMembers)
                .recruitDeadline(recruitDeadline)
                .startDate(startDate)
                .endDate(endDate)
                .weeklyGuides(weeklyGuides)
                .build();
    }
}
