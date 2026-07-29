package com.fesi.deadlinemate.domain.gatheringDraft.entity;

import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.global.common.BaseTimeEntity;
import com.fesi.deadlinemate.global.common.LongListConverter;
import com.fesi.deadlinemate.global.common.StringListConverter;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gathering_drafts", indexes = {
        @Index(name = "idx_gathering_drafts_user_id_updated_at", columnList = "userId, updatedAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GatheringDraft extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GatheringType type;

    @Convert(converter = LongListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Long> categoryIds;

    @Column(length = 60)
    private String title;

    @Column(length = 100)
    private String shortDescription;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags;

    @Column(columnDefinition = "LONGTEXT")
    private String goal;

    private Integer maxMembers;

    private LocalDate recruitDeadline;

    private LocalDate startDate;

    private LocalDate endDate;

    @Convert(converter = WeeklyGuideListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<WeeklyGuideItem> weeklyGuides;

    @Builder
    public GatheringDraft(
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
        this.userId = userId;
        this.type = type;
        this.categoryIds = categoryIds;
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.tags = tags;
        this.goal = goal;
        this.maxMembers = maxMembers;
        this.recruitDeadline = recruitDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.weeklyGuides = weeklyGuides;
    }

    public void validateOwner(Long userId) {
        if (userId == null || !this.userId.equals(userId)) {
            throw new BusinessException(ErrorCode.GATHERING_DRAFT_ACCESS_DENIED);
        }
    }

    public void update(
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
        this.type = type != null ? type : this.type;
        this.categoryIds = categoryIds != null ? categoryIds : this.categoryIds;
        this.title = title != null ? title : this.title;
        this.shortDescription = shortDescription != null ? shortDescription : this.shortDescription;
        this.description = description != null ? description : this.description;
        this.tags = tags != null ? tags : this.tags;
        this.goal = goal != null ? goal : this.goal;
        this.maxMembers = maxMembers != null ? maxMembers : this.maxMembers;
        this.recruitDeadline = recruitDeadline != null ? recruitDeadline : this.recruitDeadline;
        this.startDate = startDate != null ? startDate : this.startDate;
        this.endDate = endDate != null ? endDate : this.endDate;
        this.weeklyGuides = weeklyGuides != null ? weeklyGuides : this.weeklyGuides;
    }
}
