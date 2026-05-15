package com.fesi.deadlinemate.domain.gathering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fesi.deadlinemate.domain.category.entity.Category;
import com.fesi.deadlinemate.domain.category.entity.GatheringCategory;
import com.fesi.deadlinemate.domain.category.repository.CategoryRepository;
import com.fesi.deadlinemate.domain.category.repository.GatheringCategoryRepository;
import com.fesi.deadlinemate.domain.gathering.dto.response.MemberListResponse;
import com.fesi.deadlinemate.domain.gathering.dto.response.MyGatheringListResponse;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringTagRepository;
import com.fesi.deadlinemate.domain.gatheringApplication.entity.ApplicationStatus;
import com.fesi.deadlinemate.domain.gatheringApplication.repository.GatheringApplicationRepository;
import com.fesi.deadlinemate.domain.review.repository.ReviewRepository;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MembershipQueryServiceTest {

    @Mock private GatheringMemberRepository gatheringMemberRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringTagRepository gatheringTagRepository;
    @Mock private GatheringApplicationRepository gatheringApplicationRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private GatheringCategoryRepository gatheringCategoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserClient userClient;

    @InjectMocks
    private MembershipQueryService membershipQueryService;

    @Nested
    @DisplayName("내 모임 목록 조회")
    class GetMyGatherings {

        @Test
        @DisplayName("참여 중인 모임이 없으면 빈 목록을 반환한다")
        void emptyGatherings() {
            given(gatheringMemberRepository.findActiveGatheringIdsByUserId(1L)).willReturn(List.of());

            MyGatheringListResponse response = membershipQueryService.getMyGatherings(1L, "all", "latest", 1, 12);

            assertThat(response.gatherings()).isEmpty();
            assertThat(response.totalCount()).isZero();
        }

        @Test
        @DisplayName("참여 중인 모임 목록과 역할을 반환한다")
        void getMyGatherings() {
            Gathering gathering = gathering(1L, 3);
            GatheringMember member = member(1L, 1L, 10L, GatheringRole.MEMBER,"0.0");

            Category category = Category.builder().name("개발").build();
            setField(category, "id", 100L);

            GatheringCategory mapping = GatheringCategory.builder()
                    .gatheringId(1L)
                    .categoryId(100L)
                    .build();

            given(gatheringMemberRepository.findActiveGatheringIdsByUserId(10L)).willReturn(List.of(1L));
            given(gatheringRepository.findByIdInOrderByCreatedAtDesc(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(gathering)));
            given(gatheringMemberRepository.findByGatheringIdInAndUserIdAndIsActiveTrue(List.of(1L), 10L))
                    .willReturn(List.of(member));
            given(gatheringTagRepository.findTagRowsByGatheringIdIn(any(Collection.class)))
                    .willReturn(List.of());
            given(gatheringCategoryRepository.findByGatheringIdIn(List.of(1L)))
                    .willReturn(List.of(mapping));
            given(categoryRepository.findByIdIn(List.of(100L)))
                    .willReturn(List.of(category));
            given(reviewRepository.countReviewedMembersGroupByGathering(any(), any())).willReturn(List.of());
            given(gatheringApplicationRepository.countByGatheringIdInAndStatus(any(), any())).willReturn(List.of());

            MyGatheringListResponse response = membershipQueryService.getMyGatherings(10L, "all", "latest", 1, 12);

            assertThat(response.gatherings()).hasSize(1);
            assertThat(response.gatherings().get(0).myRole()).isEqualTo(GatheringRole.MEMBER);
            assertThat(response.gatherings().get(0).categories()).containsExactly("개발");
            assertThat(response.gatherings().get(0).hasReviewed()).isFalse();
            assertThat(response.gatherings().get(0).pendingApplicationCount()).isNull();
        }

        @Test
        @DisplayName("sort=oldest이면 오름차순으로 조회한다")
        void getMyGatheringsOldest() {
            Gathering gathering = gathering(1L, 3);
            GatheringMember member = member(1L, 1L, 10L, GatheringRole.MEMBER,"0.0");

            given(gatheringMemberRepository.findActiveGatheringIdsByUserId(10L)).willReturn(List.of(1L));
            given(gatheringRepository.findByIdInOrderByCreatedAtAsc(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(gathering)));
            given(gatheringMemberRepository.findByGatheringIdInAndUserIdAndIsActiveTrue(List.of(1L), 10L))
                    .willReturn(List.of(member));
            given(gatheringTagRepository.findTagRowsByGatheringIdIn(any(Collection.class)))
                    .willReturn(List.of());
            given(gatheringCategoryRepository.findByGatheringIdIn(any())).willReturn(List.of());
            given(reviewRepository.countReviewedMembersGroupByGathering(any(), any())).willReturn(List.of());
            given(gatheringApplicationRepository.countByGatheringIdInAndStatus(any(), any())).willReturn(List.of());

            MyGatheringListResponse response = membershipQueryService.getMyGatherings(10L, "all", "oldest", 1, 12);

            assertThat(response.gatherings()).hasSize(1);
        }

        @Test
        @DisplayName("LEADER인 모임은 pendingApplicationCount를 반환한다")
        void leaderGetsPendingCount() {
            Gathering gathering = gathering(1L, 3);
            GatheringMember member = member(1L, 1L, 10L, GatheringRole.LEADER,"0.0");

            given(gatheringMemberRepository.findActiveGatheringIdsByUserId(10L)).willReturn(List.of(1L));
            given(gatheringRepository.findByIdInOrderByCreatedAtDesc(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(gathering)));
            given(gatheringMemberRepository.findByGatheringIdInAndUserIdAndIsActiveTrue(List.of(1L), 10L))
                    .willReturn(List.of(member));
            given(gatheringTagRepository.findTagRowsByGatheringIdIn(any(Collection.class)))
                    .willReturn(List.of());
            given(gatheringCategoryRepository.findByGatheringIdIn(any())).willReturn(List.of());
            given(reviewRepository.countReviewedMembersGroupByGathering(any(), any())).willReturn(List.of());
            List<Object[]> pendingCounts = new ArrayList<>();
            pendingCounts.add(new Object[]{1L, 3L});
            given(gatheringApplicationRepository.countByGatheringIdInAndStatus(List.of(1L), ApplicationStatus.PENDING))
                    .willReturn(pendingCounts);

            MyGatheringListResponse response = membershipQueryService.getMyGatherings(10L, "all", "latest", 1, 12);

            assertThat(response.gatherings().get(0).pendingApplicationCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("리뷰를 작성한 모임은 hasReviewed가 true이다")
        void hasReviewedTrue() {
            Gathering gathering = gathering(1L, 3);
            GatheringMember member = member(1L, 1L, 10L, GatheringRole.MEMBER,"0.0");

            given(gatheringMemberRepository.findActiveGatheringIdsByUserId(10L)).willReturn(List.of(1L));
            given(gatheringRepository.findByIdInOrderByCreatedAtDesc(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(gathering)));
            given(gatheringMemberRepository.findByGatheringIdInAndUserIdAndIsActiveTrue(List.of(1L), 10L))
                    .willReturn(List.of(member));
            given(gatheringTagRepository.findTagRowsByGatheringIdIn(any(Collection.class)))
                    .willReturn(List.of());
            given(gatheringCategoryRepository.findByGatheringIdIn(any())).willReturn(List.of());
            ReviewRepository.ReviewCountRow row = new ReviewRepository.ReviewCountRow() {
                @Override public Long getGatheringId() { return 1L; }
                @Override public Long getCount() { return 1L; }
            };
            given(reviewRepository.countReviewedMembersGroupByGathering(10L, List.of(1L))).willReturn(List.of(row));
            given(gatheringApplicationRepository.countByGatheringIdInAndStatus(any(), any())).willReturn(List.of());

            MyGatheringListResponse response = membershipQueryService.getMyGatherings(10L, "all", "latest", 1, 12);

            assertThat(response.gatherings().get(0).hasReviewed()).isTrue();
            assertThat(response.gatherings().get(0).reviewedMembersCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("멤버 목록 조회")
    class GetMembers {

        @Test
        @DisplayName("배치로 유저 정보를 조회하여 멤버 목록을 반환한다")
        void getMembers() {
            GatheringMember member = member(1L, 1L, 10L, GatheringRole.LEADER,"100.0");
            given(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(1L, 10L)).willReturn(true);
            given(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(1L))
                    .willReturn(List.of(member));
            given(userClient.findByIds(List.of(10L))).willReturn(
                    Map.of(10L, UserInfo.builder().id(10L).nickname("leader").build()));

            MemberListResponse response = membershipQueryService.getMembers(1L, 10L);

            assertThat(response.members()).hasSize(1);
            assertThat(response.members().get(0).overallAchievementRate())
                    .isEqualByComparingTo("100.0");
        }

        @Test
        @DisplayName("할 일이 없는 멤버는 달성률 0.0%를 반환한다")
        void getMembersWithNoTodos() {
            GatheringMember member = member(1L, 1L, 10L, GatheringRole.MEMBER,"0.0");
            given(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(1L, 10L)).willReturn(true);
            given(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(1L))
                    .willReturn(List.of(member));
            given(userClient.findByIds(List.of(10L))).willReturn(
                    Map.of(10L, UserInfo.builder().id(10L).nickname("member").build()));

            MemberListResponse response = membershipQueryService.getMembers(1L, 10L);

            assertThat(response.members().get(0).overallAchievementRate())
                    .isEqualByComparingTo("0.0");
        }

        @Test
        @DisplayName("일부 완료된 할 일이 있으면 달성률이 정확히 계산된다")
        void getMembersWithPartialCompletion() {
            GatheringMember member = member(1L, 1L, 10L, GatheringRole.MEMBER,"75.0");
            given(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(1L, 10L)).willReturn(true);
            given(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(1L))
                    .willReturn(List.of(member));
            given(userClient.findByIds(List.of(10L))).willReturn(
                    Map.of(10L, UserInfo.builder().id(10L).nickname("member").build()));

            MemberListResponse response = membershipQueryService.getMembers(1L, 10L);

            assertThat(response.members().get(0).overallAchievementRate())
                    .isEqualByComparingTo("75.0");
        }

        @Test
        @DisplayName("여러 멤버 각각의 달성률을 독립적으로 계산한다")
        void getMembersWithMultipleMembersHavingDifferentRates() {
            GatheringMember leader = member(1L, 1L, 10L, GatheringRole.LEADER,"100.0");
            GatheringMember memberA = member(2L, 1L, 20L, GatheringRole.MEMBER,"0.0");
            GatheringMember memberB = member(3L, 1L, 30L, GatheringRole.MEMBER,"50.0");

            given(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(1L, 10L)).willReturn(true);
            given(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(1L))
                    .willReturn(List.of(leader, memberA, memberB));
            given(userClient.findByIds(List.of(10L, 20L, 30L))).willReturn(Map.of(
                    10L, UserInfo.builder().id(10L).nickname("leader").build(),
                    20L, UserInfo.builder().id(20L).nickname("memberA").build(),
                    30L, UserInfo.builder().id(30L).nickname("memberB").build()
            ));

            MemberListResponse response = membershipQueryService.getMembers(1L, 10L);

            assertThat(response.members()).hasSize(3);

            BigDecimal leaderRate = response.members().stream()
                    .filter(m -> m.userId().equals(10L)).findFirst().orElseThrow()
                    .overallAchievementRate();
            BigDecimal memberARate = response.members().stream()
                    .filter(m -> m.userId().equals(20L)).findFirst().orElseThrow()
                    .overallAchievementRate();
            BigDecimal memberBRate = response.members().stream()
                    .filter(m -> m.userId().equals(30L)).findFirst().orElseThrow()
                    .overallAchievementRate();

            assertThat(leaderRate).isEqualByComparingTo("100.0");
            assertThat(memberARate).isEqualByComparingTo("0.0");
            assertThat(memberBRate).isEqualByComparingTo("50.0");
        }

        @Test
        @DisplayName("멤버가 아닌 사용자가 조회하면 예외가 발생한다")
        void notMemberThrows() {
            given(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(1L, 99L)).willReturn(false);

            assertThatThrownBy(() -> membershipQueryService.getMembers(1L, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_A_MEMBER);
        }
    }

    private GatheringMember member(Long id, Long gatheringId, Long userId, GatheringRole role,String overallAchievementRate) {
        GatheringMember m = GatheringMember.builder()
                .gatheringId(gatheringId).userId(userId).role(role).overallAchievementRate(new BigDecimal(overallAchievementRate))
.isActive(true).build();
        setField(m, "id", id);
        return m;
    }

    private Gathering gathering(Long id, int currentMembers) {
        Gathering g = Gathering.builder()
                .leaderId(10L).type(GatheringType.STUDY)
                .title("test").shortDescription("s").description("d").goal("g")
                .maxMembers(5).currentMembers(currentMembers)
                .recruitDeadline(LocalDate.now()).startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(4))
                .totalWeeks(4).status(GatheringStatus.IN_PROGRESS).viewCount(0).build();
        setField(g, "id", id);
        return g;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}