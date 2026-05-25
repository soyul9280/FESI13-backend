package com.fesi.deadlinemate.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fesi.deadlinemate.domain.gathering.client.GatheringClient;
import com.fesi.deadlinemate.domain.gathering.client.dto.GatheringInfo;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.review.command.CreateReviewCommand;
import com.fesi.deadlinemate.domain.review.entity.Review;
import com.fesi.deadlinemate.domain.review.repository.ReviewRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private GatheringClient gatheringClient;
    @Mock private UserClient userClient;
    @InjectMocks private ReviewCommandService reviewCommandService;

    @Nested
    @DisplayName("리뷰 작성")
    class CreateReviews {

        @Test
        @DisplayName("완료된 모임에 리뷰를 작성한다")
        void createReviews() {
            given(gatheringClient.findById(1L)).willReturn(Optional.of(completedGatheringInfo()));
            given(gatheringClient.isMember(1L, 10L)).willReturn(true);
            given(gatheringClient.isMember(1L, 20L)).willReturn(true);
            given(reviewRepository.existsByGatheringIdAndReviewerIdAndTargetUserId(1L, 10L, 20L)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(20L, List.of("성실해요", "소통이 좋아요"), null, "좋았습니다")
            ));

            reviewCommandService.createReviews(command);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            then(reviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getTagCount()).isEqualTo(2);
            then(userClient).should().addReputationScore(eq(20L), any(BigDecimal.class));
        }

        @Test
        @DisplayName("완료되지 않은 모임이면 예외가 발생한다")
        void gatheringNotCompleted() {
            given(gatheringClient.findById(1L)).willReturn(Optional.empty());

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(20L, List.of("성실해요"), null, null)
            ));

            assertThatThrownBy(() -> reviewCommandService.createReviews(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.GATHERING_NOT_COMPLETED);
        }

        @Test
        @DisplayName("이미 리뷰를 작성했으면 예외가 발생한다")
        void alreadySubmitted() {
            given(gatheringClient.findById(1L)).willReturn(Optional.of(completedGatheringInfo()));
            given(gatheringClient.isMember(1L, 10L)).willReturn(true);
            given(gatheringClient.isMember(1L, 20L)).willReturn(true);
            given(reviewRepository.existsByGatheringIdAndReviewerIdAndTargetUserId(1L, 10L, 20L)).willReturn(true);

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(20L, List.of("성실해요"), null, null)
            ));

            assertThatThrownBy(() -> reviewCommandService.createReviews(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_ALREADY_SUBMITTED);
        }

        @Test
        @DisplayName("본인에게 리뷰를 작성하면 예외가 발생한다")
        void selfReviewThrows() {
            given(gatheringClient.findById(1L)).willReturn(Optional.of(completedGatheringInfo()));
            given(gatheringClient.isMember(1L, 10L)).willReturn(true);

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(10L, List.of("성실해요"), null, null)
            ));

            assertThatThrownBy(() -> reviewCommandService.createReviews(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SELF_REVIEW_NOT_ALLOWED);
        }

        @Test
        @DisplayName("모임 멤버가 아닌 사람이 리뷰하면 예외가 발생한다")
        void nonMemberReviewerThrows() {
            given(gatheringClient.findById(1L)).willReturn(Optional.of(completedGatheringInfo()));
            given(gatheringClient.isMember(1L, 10L)).willReturn(false);

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(20L, List.of("성실해요"), null, null)
            ));

            assertThatThrownBy(() -> reviewCommandService.createReviews(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REVIEWER_NOT_A_MEMBER);
        }

        @Test
        @DisplayName("리뷰 대상이 모임 멤버가 아니면 예외가 발생한다")
        void nonMemberTargetThrows() {
            given(gatheringClient.findById(1L)).willReturn(Optional.of(completedGatheringInfo()));
            given(gatheringClient.isMember(1L, 10L)).willReturn(true);
            given(gatheringClient.isMember(1L, 20L)).willReturn(false);

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(20L, List.of("성실해요"), null, null)
            ));

            assertThatThrownBy(() -> reviewCommandService.createReviews(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_TARGET_NOT_A_MEMBER);
        }

        @Test
        @DisplayName("matesTag를 포함하여 리뷰를 작성한다")
        void createReviewWithMatesTag() {
            given(gatheringClient.findById(1L)).willReturn(Optional.of(completedGatheringInfo()));
            given(gatheringClient.isMember(1L, 10L)).willReturn(true);
            given(gatheringClient.isMember(1L, 20L)).willReturn(true);
            given(reviewRepository.existsByGatheringIdAndReviewerIdAndTargetUserId(1L, 10L, 20L)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(20L, List.of("성실해요"), "불꽃", null)
            ));

            reviewCommandService.createReviews(command);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            then(reviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getMatesTag()).isEqualTo("불꽃");
        }

        @Test
        @DisplayName("유효하지 않은 matesTag는 예외를 발생시킨다")
        void invalidMatesTagThrowsException() {
            given(gatheringClient.findById(1L)).willReturn(Optional.of(completedGatheringInfo()));
            given(gatheringClient.isMember(1L, 10L)).willReturn(true);
            given(gatheringClient.isMember(1L, 20L)).willReturn(true);

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(20L, List.of("성실해요"), "잘못된태그", null)
            ));

            assertThatThrownBy(() -> reviewCommandService.createReviews(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MATES_TAG);
        }

        @Test
        @DisplayName("태그 수에 비례하여 UserClient.addReputationScore가 호출된다")
        void reputationScoreViaClient() {
            given(gatheringClient.findById(1L)).willReturn(Optional.of(completedGatheringInfo()));
            given(gatheringClient.isMember(1L, 10L)).willReturn(true);
            given(gatheringClient.isMember(1L, 20L)).willReturn(true);
            given(reviewRepository.existsByGatheringIdAndReviewerIdAndTargetUserId(1L, 10L, 20L)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

            CreateReviewCommand command = new CreateReviewCommand(1L, 10L, List.of(
                    new CreateReviewCommand.ReviewItem(20L, List.of("성실해요", "소통이 좋아요", "잘 도와줘요"), null, null)
            ));

            reviewCommandService.createReviews(command);

            then(userClient).should().addReputationScore(20L, BigDecimal.valueOf(0.3));
        }
    }

    private GatheringInfo completedGatheringInfo() {
        return GatheringInfo.builder()
                .id(1L).title("Test Gathering").status(GatheringStatus.COMPLETED).build();
    }
}
