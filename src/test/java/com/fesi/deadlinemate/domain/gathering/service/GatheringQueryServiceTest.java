package com.fesi.deadlinemate.domain.gathering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fesi.deadlinemate.domain.category.entity.Category;
import com.fesi.deadlinemate.domain.category.entity.GatheringCategory;
import com.fesi.deadlinemate.domain.category.repository.CategoryRepository;
import com.fesi.deadlinemate.domain.category.repository.GatheringCategoryRepository;
import com.fesi.deadlinemate.domain.gathering.dto.request.GatheringSearchCondition;
import com.fesi.deadlinemate.domain.gathering.dto.response.GatheringDetailResponse;
import com.fesi.deadlinemate.domain.gathering.dto.response.GatheringListResponse;
import com.fesi.deadlinemate.domain.gathering.dto.response.GatheringMainResponse;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gathering.entity.WeeklyPlan;
import com.fesi.deadlinemate.domain.gathering.projection.GatheringDetailRow;
import com.fesi.deadlinemate.domain.gathering.projection.GatheringListRow;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringImageRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringTagRepository;
import com.fesi.deadlinemate.domain.gathering.repository.WeeklyPlanDetailRepository;
import com.fesi.deadlinemate.domain.gathering.repository.WeeklyPlanRepository;
import com.fesi.deadlinemate.domain.like.repository.GatheringLikeRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class GatheringQueryServiceTest {

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringTagRepository gatheringTagRepository;
    @Mock private GatheringImageRepository gatheringImageRepository;
    @Mock private GatheringCategoryRepository gatheringCategoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private WeeklyPlanRepository weeklyPlanRepository;
    @Mock private WeeklyPlanDetailRepository weeklyPlanDetailRepository;
    @Mock private GatheringMemberRepository gatheringMemberRepository;
    @Mock private GatheringLikeRepository gatheringLikeRepository;
    @Mock private UserClient userClient;

    @InjectMocks
    private GatheringQueryService gatheringQueryService;

    @Nested
    @DisplayName("모임 목록 조회")
    class GetGatherings {

        @Test
        @DisplayName("검색 조건으로 모임 목록을 페이징 조회할 수 있다")
        void returnsPagedGatheringList() {
            GatheringListRow row = sampleRow(10L, 1L);
            Page<GatheringListRow> page = new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1);
            given(gatheringRepository.search(any(), any(Pageable.class))).willReturn(page);
            mockListHelperDeps(List.of(10L), 1L);

            GatheringListResponse response = gatheringQueryService.getGatherings(
                    GatheringSearchCondition.builder().build(), 1, 10);

            assertThat(response.gatherings()).hasSize(1);
            assertThat(response.totalCount()).isEqualTo(1L);
            assertThat(response.currentPage()).isEqualTo(1);
            assertThat(response.gatherings().get(0).title()).isEqualTo("React 스터디");
        }

        @Test
        @DisplayName("page가 0 이하이면 1로 보정된다")
        void pageZeroIsNormalizedToOne() {
            Page<GatheringListRow> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(gatheringRepository.search(any(), any(Pageable.class))).willReturn(emptyPage);

            GatheringListResponse response = gatheringQueryService.getGatherings(
                    GatheringSearchCondition.builder().build(), 0, 10);

            assertThat(response.currentPage()).isEqualTo(1);
            assertThat(response.gatherings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("메인 화면 모임 조회")
    class GetMainGatherings {

        @Test
        @DisplayName("인기·마감임박·최신 섹션을 각각 반환한다")
        void returnsAllMainSections() {
            GatheringListRow row = sampleRow(10L, 1L);
            given(gatheringRepository.findMainPopular(5)).willReturn(List.of(row));
            given(gatheringRepository.findMainDeadline(5)).willReturn(List.of());
            given(gatheringRepository.findMainLatest(5)).willReturn(List.of());
            mockListHelperDeps(List.of(10L), 1L);

            GatheringMainResponse response = gatheringQueryService.getMainGatherings(5);

            assertThat(response.popular()).hasSize(1);
            assertThat(response.popular().get(0).title()).isEqualTo("React 스터디");
            assertThat(response.deadline()).isEmpty();
            assertThat(response.latest()).isEmpty();
        }

        @Test
        @DisplayName("3개 섹션 모두에 데이터가 있을 때 배치 쿼리가 1회만 실행된다")
        void fetchesRelatedDataOnceForAllSections() {
            GatheringListRow row1 = sampleRow(1L, 10L);
            GatheringListRow row2 = sampleRow(2L, 20L);
            GatheringListRow row3 = sampleRow(3L, 30L);
            given(gatheringRepository.findMainPopular(5)).willReturn(List.of(row1));
            given(gatheringRepository.findMainDeadline(5)).willReturn(List.of(row2));
            given(gatheringRepository.findMainLatest(5)).willReturn(List.of(row3));

            List<Long> allIds = List.of(1L, 2L, 3L);
            given(gatheringTagRepository.findTagRowsByGatheringIdIn(allIds))
                    .willReturn(List.of());
            given(gatheringCategoryRepository.findByGatheringIdIn(allIds))
                    .willReturn(List.of());
            given(categoryRepository.findByIdIn(anyList())).willReturn(List.of());
            given(userClient.findByIds(anyList())).willReturn(Map.of(
                    10L, UserInfo.builder().id(10L).nickname("리더1").profileImage(null).build(),
                    20L, UserInfo.builder().id(20L).nickname("리더2").profileImage(null).build(),
                    30L, UserInfo.builder().id(30L).nickname("리더3").profileImage(null).build()
            ));

            GatheringMainResponse response = gatheringQueryService.getMainGatherings(5);

            assertThat(response.popular()).hasSize(1);
            assertThat(response.deadline()).hasSize(1);
            assertThat(response.latest()).hasSize(1);
            verify(gatheringTagRepository, times(1))
                    .findTagRowsByGatheringIdIn(allIds);
            verify(gatheringCategoryRepository, times(1))
                    .findByGatheringIdIn(allIds);
            verify(userClient, times(1)).findByIds(anyList());
        }
    }

    @Nested
    @DisplayName("좋아요한 모임 목록 조회")
    class GetMyLikedGatherings {

        @Test
        @DisplayName("좋아요한 모임 목록을 반환한다")
        void returnsLikedGatherings() {
            given(gatheringLikeRepository.findGatheringIdsByUserId(1L)).willReturn(List.of(10L));
            Page<GatheringListRow> page = new PageImpl<>(List.of(sampleRow(10L, 2L)), PageRequest.of(0, 10), 1);
            given(gatheringRepository.findByIdIn(eq(List.of(10L)), any(Pageable.class))).willReturn(page);
            mockListHelperDeps(List.of(10L), 2L);

            GatheringListResponse response = gatheringQueryService.getMyLikedGatherings(1L, 1, 10);

            assertThat(response.gatherings()).hasSize(1);
            assertThat(response.totalCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("좋아요한 모임이 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoLikes() {
            given(gatheringLikeRepository.findGatheringIdsByUserId(1L)).willReturn(List.of());
            Page<GatheringListRow> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(gatheringRepository.findByIdIn(eq(List.of()), any(Pageable.class))).willReturn(emptyPage);

            GatheringListResponse response = gatheringQueryService.getMyLikedGatherings(1L, 1, 10);

            assertThat(response.gatherings()).isEmpty();
            assertThat(response.totalCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("모임 상세 조회")
    class GetGatheringDetail {

        @Test
        @DisplayName("모임 상세 정보를 반환한다")
        void returnsGatheringDetail() {
            GatheringDetailRow row = GatheringDetailRow.builder()
                    .id(10L).leaderId(1L).type(GatheringType.STUDY)
                    .title("React 스터디").shortDescription("짧은 소개").description("상세 설명")
                    .goal("목표").maxMembers(6).currentMembers(2)
                    .recruitDeadline(LocalDate.of(2025, 3, 20))
                    .startDate(LocalDate.of(2025, 3, 22)).endDate(LocalDate.of(2025, 4, 19))
                    .totalWeeks(5).status(GatheringStatus.RECRUITING)
                    .build();
            given(gatheringRepository.findDetailRowById(10L)).willReturn(Optional.of(row));

            GatheringCategory gc = GatheringCategory.builder().gatheringId(10L).categoryId(1L).build();
            given(gatheringCategoryRepository.findByGatheringId(10L)).willReturn(List.of(gc));
            Category cat = Category.builder().name("개발").build();
            setField(cat, "id", 1L);
            given(categoryRepository.findByIdIn(List.of(1L))).willReturn(List.of(cat));

            given(gatheringTagRepository.findTagsByGatheringId(10L)).willReturn(List.of("React"));
            given(gatheringImageRepository.findImageRowsByGatheringId(10L)).willReturn(List.of());

            WeeklyPlan plan = WeeklyPlan.builder()
                    .gatheringId(10L).weekNumber(1).title("1주차")
                    .startDate(LocalDate.of(2025, 3, 22)).endDate(LocalDate.of(2025, 3, 28))
                    .build();
            setField(plan, "id", 11L);
            given(weeklyPlanRepository.findByGatheringIdOrderByWeekNumberAsc(10L)).willReturn(List.of(plan));
            given(weeklyPlanDetailRepository.findByWeeklyPlanIdInOrderByWeeklyPlanIdAscDisplayOrderAsc(List.of(11L)))
                    .willReturn(List.of());

            GatheringMember member = GatheringMember.builder()
                    .gatheringId(10L).userId(1L).role(GatheringRole.LEADER).build();
            given(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(10L))
                    .willReturn(List.of(member));

            UserInfo leaderInfo = UserInfo.builder().id(1L).nickname("리더").profileImage(null).build();
            given(userClient.findByIds(List.of(1L))).willReturn(Map.of(1L, leaderInfo));
            given(userClient.findById(1L)).willReturn(leaderInfo);

            GatheringDetailResponse response = gatheringQueryService.getGatheringDetail(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.title()).isEqualTo("React 스터디");
            assertThat(response.categories()).containsExactly("개발");
            assertThat(response.tags()).containsExactly("React");
            assertThat(response.leader().nickname()).isEqualTo("리더");
            assertThat(response.members()).hasSize(1);
            assertThat(response.weeklyPlans()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 모임 조회 시 예외가 발생한다")
        void throwsWhenGatheringNotFound() {
            given(gatheringRepository.findDetailRowById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> gatheringQueryService.getGatheringDetail(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.GATHERING_NOT_FOUND);
        }
    }


    // ---- helpers ----

    private GatheringListRow sampleRow(Long id, Long leaderId) {
        return GatheringListRow.builder()
                .id(id).leaderId(leaderId).type(GatheringType.STUDY)
                .title("React 스터디").shortDescription("짧은 소개")
                .maxMembers(6).currentMembers(2)
                .recruitDeadline(LocalDate.of(2025, 3, 20))
                .startDate(LocalDate.of(2025, 3, 22)).endDate(LocalDate.of(2025, 4, 19))
                .status(GatheringStatus.RECRUITING)
                .build();
    }


    /**
     * {@code toListItemResponses} 내부 호출되는 공통 의존성 모킹.
     * gatheringIds 리스트에 아이템이 있을 때 호출되는 tag/category/user 레포 스텁을 설정한다.
     */
    private void mockListHelperDeps(List<Long> gatheringIds, Long leaderId) {
        given(gatheringTagRepository.findTagRowsByGatheringIdIn(gatheringIds))
                .willReturn(List.of());
        given(gatheringCategoryRepository.findByGatheringIdIn(gatheringIds))
                .willReturn(List.of());
        given(categoryRepository.findByIdIn(anyList()))
                .willReturn(List.of());
        given(userClient.findByIds(List.of(leaderId)))
                .willReturn(Map.of(leaderId, UserInfo.builder().id(leaderId).nickname("리더").profileImage(null).build()));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
