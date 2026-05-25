package com.fesi.deadlinemate.domain.review.service;

import com.fesi.deadlinemate.domain.gathering.client.GatheringClient;
import com.fesi.deadlinemate.domain.review.command.CreateReviewCommand;
import com.fesi.deadlinemate.domain.review.entity.Review;
import com.fesi.deadlinemate.domain.review.repository.ReviewRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private static final BigDecimal REPUTATION_DELTA_PER_TAG = BigDecimal.valueOf(0.1);
    private static final Set<String> VALID_MATES_TAGS = Set.of("연기", "불씨", "불꽃", "태양");

    private final ReviewRepository reviewRepository;
    private final GatheringClient gatheringClient;
    private final UserClient userClient;

    public void createReviews(CreateReviewCommand command) {
        validateGatheringCompleted(command.gatheringId());
        validateReviewerIsMember(command.gatheringId(), command.reviewerId());

        command.reviews().forEach(item -> {
            validateNotSelfReview(command.reviewerId(), item.targetUserId());
            validateTargetIsMember(command.gatheringId(), item.targetUserId());
            validateMatesTag(item.matesTag());

            if (reviewRepository.existsByGatheringIdAndReviewerIdAndTargetUserId(
                    command.gatheringId(), command.reviewerId(), item.targetUserId())) {
                throw new BusinessException(ErrorCode.REVIEW_ALREADY_SUBMITTED);
            }

            Review review = Review.create(
                    command.gatheringId(), command.reviewerId(),
                    item.targetUserId(), item.tags(), item.matesTag(), item.comment()
            );

            reviewRepository.save(review);

            BigDecimal delta = REPUTATION_DELTA_PER_TAG.multiply(BigDecimal.valueOf(review.getTagCount()));
            userClient.addReputationScore(item.targetUserId(), delta);
        });
    }

    private void validateGatheringCompleted(Long gatheringId) {
        gatheringClient.findById(gatheringId)
                .filter(info -> info.isCompleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_COMPLETED));
    }

    private void validateReviewerIsMember(Long gatheringId, Long reviewerId) {
        if (!gatheringClient.isMember(gatheringId, reviewerId)) {
            throw new BusinessException(ErrorCode.REVIEWER_NOT_A_MEMBER);
        }
    }

    private void validateTargetIsMember(Long gatheringId, Long targetUserId) {
        if (!gatheringClient.isMember(gatheringId, targetUserId)) {
            throw new BusinessException(ErrorCode.REVIEW_TARGET_NOT_A_MEMBER);
        }
    }

    private void validateMatesTag(String matesTag) {
        if (matesTag != null && !VALID_MATES_TAGS.contains(matesTag)) {
            throw new BusinessException(ErrorCode.INVALID_MATES_TAG);
        }
    }

    private void validateNotSelfReview(Long reviewerId, Long targetUserId) {
        if (reviewerId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.SELF_REVIEW_NOT_ALLOWED);
        }
    }
}
