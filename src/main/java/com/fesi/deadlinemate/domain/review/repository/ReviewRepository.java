package com.fesi.deadlinemate.domain.review.repository;

import com.fesi.deadlinemate.domain.review.entity.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByGatheringIdAndReviewerId(Long gatheringId, Long reviewerId);

    Page<Review> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);

    List<Review> findByTargetUserIdInOrderByCreatedAtDesc(List<Long> targetUserIds);

    @Query("SELECT r.matesTag, COUNT(r) FROM Review r WHERE r.targetUserId = :targetUserId AND r.matesTag IS NOT NULL GROUP BY r.matesTag")
    List<Object[]> countMatesTagsByTargetUserId(@Param("targetUserId") Long targetUserId);

    @Query("SELECT r.gatheringId AS gatheringId, COUNT(r) AS count FROM Review r "
            + "WHERE r.reviewerId = :reviewerId AND r.gatheringId IN :gatheringIds "
            + "GROUP BY r.gatheringId")
    List<ReviewCountRow> countReviewedMembersGroupByGathering(
            @Param("reviewerId") Long reviewerId,
            @Param("gatheringIds") List<Long> gatheringIds);

    interface ReviewCountRow {
        Long getGatheringId();
        Long getCount();
    }

    long countByTargetUserId(Long targetUserId);
}
