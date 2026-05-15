package com.fesi.deadlinemate.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fesi.deadlinemate.domain.review.entity.Review;
import com.fesi.deadlinemate.domain.review.entity.ReviewTag;
import com.fesi.deadlinemate.global.config.JpaConfig;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
    }

    @Nested
    @DisplayName("리뷰 중복 작성 확인")
    class ExistsByGatheringAndReviewer {

        @Test
        @DisplayName("같은 모임에서 같은 리뷰어의 리뷰가 존재하면 true를 반환한다")
        void existsWhenAlreadyReviewed() {
            // given
            reviewRepository.save(review(1L, 1L, 2L, List.of(ReviewTag.DILIGENT)));

            // when
            boolean exists = reviewRepository.existsByGatheringIdAndReviewerId(1L, 1L);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("리뷰가 없으면 false를 반환한다")
        void notExistsWhenNoReview() {
            // when
            boolean exists = reviewRepository.existsByGatheringIdAndReviewerId(1L, 1L);

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("다른 모임의 리뷰는 중복으로 판단하지 않는다")
        void differentGatheringIsNotDuplicate() {
            // given
            reviewRepository.save(review(2L, 1L, 2L, List.of(ReviewTag.DILIGENT)));

            // when
            boolean exists = reviewRepository.existsByGatheringIdAndReviewerId(1L, 1L);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("대상 유저 리뷰 목록 조회")
    class FindByTargetUserId {

        @Test
        @DisplayName("대상 유저가 받은 리뷰만 반환한다")
        void findByTargetUserId() {
            // given
            reviewRepository.saveAll(List.of(
                    review(1L, 1L, 2L, List.of(ReviewTag.DILIGENT)),
                    review(1L, 3L, 2L, List.of(ReviewTag.PUNCTUAL)),
                    review(1L, 1L, 3L, List.of(ReviewTag.HELPFUL))  // 다른 대상
            ));

            // when
            Page<Review> result = reviewRepository
                    .findByTargetUserIdOrderByCreatedAtDesc(2L, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(r -> r.getTargetUserId().equals(2L));
        }

        @Test
        @DisplayName("페이지네이션이 적용된다")
        void pagination() {
            // given
            for (long reviewerId = 1; reviewerId <= 5; reviewerId++) {
                reviewRepository.save(review(1L, reviewerId, 2L, List.of(ReviewTag.DILIGENT)));
            }

            // when
            Page<Review> result = reviewRepository
                    .findByTargetUserIdOrderByCreatedAtDesc(2L, PageRequest.of(0, 3));

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("리뷰에 태그가 저장되고 조회된다")
        void tagsArePersisted() {
            // given
            reviewRepository.save(review(1L, 1L, 2L,
                    List.of(ReviewTag.DILIGENT, ReviewTag.GOOD_COMMUNICATION)));

            // when
            Review found = reviewRepository
                    .findByTargetUserIdOrderByCreatedAtDesc(2L, PageRequest.of(0, 10))
                    .getContent().get(0);

            // then
            assertThat(found.getTags())
                    .containsExactlyInAnyOrder(ReviewTag.DILIGENT, ReviewTag.GOOD_COMMUNICATION);
        }
    }

    @Nested
    @DisplayName("모임별 리뷰 작성 수 집계")
    class CountReviewedMembersGroupByGathering {

        @Test
        @DisplayName("모임별 리뷰 작성 수를 반환한다")
        void countsPerGathering() {
            // given
            reviewRepository.saveAll(List.of(
                    review(1L, 1L, 2L, List.of(ReviewTag.DILIGENT)),
                    review(1L, 1L, 3L, List.of(ReviewTag.PUNCTUAL)),
                    review(2L, 1L, 2L, List.of(ReviewTag.HELPFUL))
            ));

            // when
            List<ReviewRepository.ReviewCountRow> result =
                    reviewRepository.countReviewedMembersGroupByGathering(1L, List.of(1L, 2L, 3L));

            // then
            Map<Long, Long> countMap = result.stream()
                    .collect(Collectors.toMap(
                            ReviewRepository.ReviewCountRow::getGatheringId,
                            ReviewRepository.ReviewCountRow::getCount
                    ));
            assertThat(countMap).containsEntry(1L, 2L);
            assertThat(countMap).containsEntry(2L, 1L);
            assertThat(countMap).doesNotContainKey(3L);
        }

        @Test
        @DisplayName("조회 대상 목록에 없는 모임은 반환하지 않는다")
        void excludesGatheringsNotInList() {
            // given
            reviewRepository.save(review(1L, 1L, 2L, List.of(ReviewTag.DILIGENT)));

            // when
            List<ReviewRepository.ReviewCountRow> result =
                    reviewRepository.countReviewedMembersGroupByGathering(1L, List.of(2L, 3L));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("matesTag 집계")
    class CountMatesTagsByTargetUserId {

        @Test
        @DisplayName("matesTag별 수신 횟수를 집계한다")
        void countMatesTagsByTargetUserId() {
            // given
            reviewRepository.saveAll(List.of(
                    reviewWithMatesTag(1L, 1L, 2L, "불꽃"),
                    reviewWithMatesTag(2L, 3L, 2L, "불꽃"),
                    reviewWithMatesTag(3L, 4L, 2L, "연기")
            ));

            // when
            List<Object[]> result = reviewRepository.countMatesTagsByTargetUserId(2L);

            // then
            Map<String, Long> countMap = result.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            row -> (String) row[0],
                            row -> (Long) row[1]
                    ));
            assertThat(countMap).containsEntry("불꽃", 2L);
            assertThat(countMap).containsEntry("연기", 1L);
        }

        @Test
        @DisplayName("matesTag가 null인 리뷰는 집계에서 제외된다")
        void excludesNullMatesTag() {
            // given
            reviewRepository.save(review(1L, 1L, 2L, List.of(ReviewTag.DILIGENT)));

            // when
            List<Object[]> result = reviewRepository.countMatesTagsByTargetUserId(2L);

            // then
            assertThat(result).isEmpty();
        }
    }

    private Review review(Long gatheringId, Long reviewerId, Long targetUserId, List<ReviewTag> tags) {
        List<String> tagDisplayNames = tags.stream().map(ReviewTag::getDisplayName).toList();
        return Review.create(gatheringId, reviewerId, targetUserId, tagDisplayNames, null, "테스트 리뷰");
    }

    private Review reviewWithMatesTag(Long gatheringId, Long reviewerId, Long targetUserId, String matesTag) {
        return Review.create(gatheringId, reviewerId, targetUserId,
                List.of(ReviewTag.DILIGENT.getDisplayName()), matesTag, "테스트 리뷰");
    }
}
