package com.fesi.deadlinemate.domain.gathering.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fesi.deadlinemate.domain.gathering.command.CreateGatheringCommand;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateGatheringRequest(
        @Schema(example = "STUDY", description = "모임 유형 (STUDY | PROJECT)")
        @NotNull(message = "모임 유형은 필수입니다.")
        GatheringType type,

        @NotEmpty(message = "카테고리는 최소 1개 이상 선택해야 합니다.")
        @Size(max = 3, message = "카테고리는 최대 3개까지 선택할 수 있습니다.")
        @ArraySchema(schema = @Schema(type = "integer", format = "int64", example = "1"))
        List<@NotNull(message = "카테고리 ID는 비어 있을 수 없습니다.") Long> categoryIds,

        @Schema(example = "React 스터디 모집합니다")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(min = 2, max = 30, message = "제목은 2자 이상 30자 이하여야 합니다.")
        String title,

        @Schema(example = "함께 React를 공부할 팀원을 구합니다")
        @NotBlank(message = "한 줄 소개는 필수입니다.")
        @Size(min = 2, max = 50, message = "한 줄 소개는 2자 이상 50자 이하여야 합니다.")
        String shortDescription,

        @Schema(example = "매주 React 공식 문서를 읽고 실습 프로젝트를 진행합니다. 기초부터 차근차근 같이 공부해요.")
        @Size(min = 10, max = 1000, message = "상세 설명은 10자 이상 1000자 이하여야 합니다.")
        String description,

        @Schema(example = "React 기본기 완성 및 토이 프로젝트 1개 완료")
        @NotBlank(message = "최종 목표는 필수입니다.")
        String goal,

        @ArraySchema(schema = @Schema(example = "React"))
        @NotEmpty(message = "태그는 최소 1개 이상 선택해야 합니다.")
        @Size(max = 10, message = "태그는 최대 10개까지 가능합니다.")
        List<@NotBlank(message = "태그는 비어 있을 수 없습니다.") @Size(max = 15, message = "태그는 15자 이하여야 합니다.") String> tags,

        @Schema(example = "6")
        @NotNull(message = "최대 인원은 필수입니다.")
        @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다.")
        @Max(value = 10, message = "최대 인원은 10명 이하여야 합니다.")
        int maxMembers,

        @Schema(example = "2026-05-15", type = "string", format = "date")
        @NotNull(message = "모집 마감일은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate recruitDeadline,

        @Schema(example = "2026-05-20", type = "string", format = "date")
        @NotNull(message = "시작일은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @Schema(example = "2026-08-20", type = "string", format = "date")
        @NotNull(message = "종료일은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        @Valid
        List<WeeklyGuideRequest> weeklyGuides
) {
    public CreateGatheringCommand toCommand(Long leaderId, List<String> imageUrls) {
        return CreateGatheringCommand.builder()
                .leaderId(leaderId)
                .type(type)
                .categoryIds(categoryIds == null ? List.of() : categoryIds)
                .title(title)
                .shortDescription(shortDescription)
                .description(description)
                .tags(tags == null ? List.of() : tags)
                .goal(goal)
                .maxMembers(maxMembers)
                .recruitDeadline(recruitDeadline)
                .startDate(startDate)
                .endDate(endDate)
                .weeklyGuides(
                        weeklyGuides == null ? List.of() : weeklyGuides.stream()
                                .map(w -> new CreateGatheringCommand.CreateWeeklyGuideCommand(
                                        w.week(),
                                        w.title(),
                                        w.details() == null ? List.of() : w.details()
                                ))
                                .toList()
                )
                .imageUrls(imageUrls)
                .build();
    }

    public record WeeklyGuideRequest(
            @Schema(example = "1")
            @Min(value = 1, message = "주차는 1 이상이어야 합니다.")
            int week,

            @Schema(example = "React 기초 - JSX와 컴포넌트")
            @Size(max = 100, message = "주차 제목은 100자 이하여야 합니다.")
            String title,

            @ArraySchema(schema = @Schema(example = "JSX 문법 학습 및 실습"))
            @Size(max = 2, message = "세부 계획은 최대 2개까지 입력할 수 있습니다.")
            List<@NotBlank(message = "세부 계획은 비어 있을 수 없습니다.")
            @Size(max = 200, message = "세부 계획은 200자 이하여야 합니다.") String> details
    ) {
    }
}
