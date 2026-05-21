package com.fesi.deadlinemate.domain.report.entity;

import com.fesi.deadlinemate.global.common.BaseTimeEntity;
import com.fesi.deadlinemate.global.common.LongListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gathering_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GatheringReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long gatheringId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal teamOverallRate;

    @Convert(converter = LongListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Long> mvpUserIds;

    @Convert(converter = LongListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Long> longestStreakUserIds;

    @Convert(converter = LongListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Long> mostImprovedUserIds;

    @Convert(converter = LongListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Long> attendanceUserIds;

    @Column(nullable = false)
    private int longestStreakValue;

    @Lob
    @Column(nullable = false, columnDefinition = "json")
    private String weeklyRates;

    @Builder
    public GatheringReport(
            Long gatheringId,
            BigDecimal teamOverallRate,
            List<Long> mvpUserIds,
            List<Long> longestStreakUserIds,
            int longestStreakValue,
            List<Long> mostImprovedUserIds,
            List<Long> attendanceUserIds,
            String weeklyRates
    ) {
        this.gatheringId = gatheringId;
        this.teamOverallRate = teamOverallRate;
        this.mvpUserIds = mvpUserIds;
        this.longestStreakUserIds = longestStreakUserIds;
        this.longestStreakValue = longestStreakValue;
        this.mostImprovedUserIds = mostImprovedUserIds;
        this.attendanceUserIds = attendanceUserIds;
        this.weeklyRates = weeklyRates;
    }
}
