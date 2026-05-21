package com.fesi.deadlinemate.domain.gathering.entity;

import com.fesi.deadlinemate.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gathering_tags",
        indexes = {
                @Index(name = "idx_gathering_tags_gathering_id", columnList = "gatheringId")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GatheringTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gatheringId;

    @Column(nullable = false, length = 30)
    private String tag;

    @Builder
    public GatheringTag(Long gatheringId, String tag) {
        this.gatheringId = gatheringId;
        this.tag = tag;
    }
}
