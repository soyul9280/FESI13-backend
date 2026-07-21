package com.fesi.deadlinemate.domain.gathering.entity;

import com.fesi.deadlinemate.global.common.BaseTimeEntity;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gatherings", indexes = {
        @Index(name = "idx_gatherings_status_created_at", columnList = "status, createdAt"),
        @Index(name = "idx_gatherings_recruit_deadline", columnList = "recruitDeadline"),
        @Index(name = "idx_gatherings_view_count", columnList = "viewCount")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gathering extends BaseTimeEntity {

    private static final int MIN_MEMBERS = 2;
    private static final int MAX_MEMBERS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long leaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GatheringType type;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false, length = 100)
    private String shortDescription;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String goal;

    @Column(nullable = false)
    private int maxMembers;

    @Column(nullable = false)
    private int currentMembers;

    @Column(nullable = false)
    private LocalDate recruitDeadline;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int totalWeeks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GatheringStatus status;

    @Column(nullable = false)
    private int viewCount;

    @Builder
    public Gathering(
            Long leaderId,
            GatheringType type,
            String title,
            String shortDescription,
            String description,
            String goal,
            int maxMembers,
            int currentMembers,
            LocalDate recruitDeadline,
            LocalDate startDate,
            LocalDate endDate,
            int totalWeeks,
            GatheringStatus status,
            int viewCount
    ) {
        validateMaxMembers(maxMembers);
        validateRecruitDeadline(recruitDeadline, startDate);
        validateDateRange(startDate, endDate);

        this.leaderId = leaderId;
        this.type = type;
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.goal = goal;
        this.maxMembers = maxMembers;
        this.currentMembers = currentMembers;
        this.recruitDeadline = recruitDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalWeeks = totalWeeks;
        this.status = status;
        this.viewCount = viewCount;
    }

    public void validateLeader(Long requesterId) {
        if (requesterId == null || !this.leaderId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.INVALID_GATHERING_LEADER);
        }
    }

    public void validateDeletable() {
        if (this.status == GatheringStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.GATHERING_DELETE_NOT_ALLOWED);
        }
    }

    public void updateRecruiting(
            GatheringType type,
            String title,
            String shortDescription,
            String description,
            String goal,
            int maxMembers,
            LocalDate recruitDeadline,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ensureRecruitingStatus();
        validateMaxMembers(maxMembers);
        validateRecruitDeadline(recruitDeadline, startDate);
        validateDateRange(startDate, endDate);

        this.type = type;
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.goal = goal;
        this.maxMembers = maxMembers;
        this.recruitDeadline = recruitDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalWeeks = calculateTotalWeeks(startDate, endDate);
    }

    public void updateInProgress(
            GatheringType type,
            String title,
            String shortDescription,
            String description,
            String goal,
            int maxMembers,
            LocalDate recruitDeadline,
            LocalDate startDate,
            LocalDate endDate,
            List<Long> requestedCategoryIds,
            List<Long> currentCategoryIds,
            List<String> requestedTags,
            List<String> currentTags
    ) {
        ensureInProgressStatus();
        validateDateRange(this.startDate, endDate);
        validateInProgressImmutableFields(
                type,
                title,
                shortDescription,
                goal,
                maxMembers,
                recruitDeadline,
                startDate,
                requestedCategoryIds,
                currentCategoryIds,
                requestedTags,
                currentTags
        );

        this.description = description;
        this.endDate = endDate;
        this.totalWeeks = calculateTotalWeeks(this.startDate, endDate);
    }

    public void start() {
        this.status = GatheringStatus.IN_PROGRESS;
    }

    public void complete() {
        if (this.status != GatheringStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.GATHERING_COMPLETE_NOT_ALLOWED);
        }
        this.status = GatheringStatus.COMPLETED;
    }

    public void validateRecruiting() {
        if (this.status != GatheringStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.GATHERING_NOT_RECRUITING);
        }
    }

    public void validateCapacity() {
        if (this.currentMembers >= this.maxMembers) {
            throw new BusinessException(ErrorCode.GATHERING_FULL);
        }
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    private void ensureRecruitingStatus() {
        if (this.status != GatheringStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.GATHERING_UPDATE_FORBIDDEN_IN_PROGRESS);
        }
    }

    private void ensureInProgressStatus() {
        if (this.status != GatheringStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.GATHERING_UPDATE_FORBIDDEN_IN_PROGRESS);
        }
    }

    private void validateInProgressImmutableFields(
            GatheringType type,
            String title,
            String shortDescription,
            String goal,
            int maxMembers,
            LocalDate recruitDeadline,
            LocalDate startDate,
            List<Long> requestedCategoryIds,
            List<Long> currentCategoryIds,
            List<String> requestedTags,
            List<String> currentTags
    ) {
        boolean typeChanged = type != this.type;
        boolean titleChanged = !Objects.equals(title, this.title);
        boolean shortDescriptionChanged = !Objects.equals(shortDescription, this.shortDescription);
        boolean goalChanged = !Objects.equals(goal, this.goal);
        boolean maxMembersChanged = maxMembers != this.maxMembers;
        boolean recruitDeadlineChanged = !Objects.equals(recruitDeadline, this.recruitDeadline);
        boolean startDateChanged = !Objects.equals(startDate, this.startDate);
        boolean categoriesChanged = !new LinkedHashSet<>(requestedCategoryIds)
                .equals(new LinkedHashSet<>(currentCategoryIds));
        boolean tagsChanged = !new LinkedHashSet<>(requestedTags)
                .equals(new LinkedHashSet<>(currentTags));

        if (typeChanged || categoriesChanged || titleChanged || shortDescriptionChanged
                || goalChanged || maxMembersChanged || recruitDeadlineChanged
                || startDateChanged || tagsChanged) {
            throw new BusinessException(ErrorCode.INVALID_IN_PROGRESS_UPDATE_ITEMS);
        }
    }

    private static void validateMaxMembers(int maxMembers) {
        if (maxMembers < MIN_MEMBERS || maxMembers > MAX_MEMBERS) {
            throw new BusinessException(ErrorCode.INVALID_MAX_MEMBERS);
        }
    }

    private static void validateRecruitDeadline(LocalDate recruitDeadline, LocalDate startDate) {
        if (recruitDeadline.isAfter(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_RECRUIT_DEADLINE);
        }
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_GATHERING_DATE);
        }
    }

    private static int calculateTotalWeeks(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return (int) (days / 7) + 1;
    }

    public void increaseCurrentMembers() {
        this.currentMembers++;
    }
}