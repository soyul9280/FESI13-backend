package com.fesi.deadlinemate.domain.gathering.entity;

import com.fesi.deadlinemate.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weekly_plans",
        indexes = {
                @Index(name = "idx_weekly_plans_gathering_id", columnList = "gatheringId")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyPlan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gatheringId;

    @Column(nullable = false)
    private int weekNumber;

    @Column(length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Builder
    public WeeklyPlan(Long gatheringId, int weekNumber, String title,
                      LocalDate startDate, LocalDate endDate) {
        this.gatheringId = gatheringId;
        this.weekNumber = weekNumber;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
