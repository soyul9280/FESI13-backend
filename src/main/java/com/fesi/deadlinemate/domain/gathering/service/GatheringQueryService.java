package com.fesi.deadlinemate.domain.gathering.service;

import com.fesi.deadlinemate.domain.category.entity.Category;
import com.fesi.deadlinemate.domain.category.entity.GatheringCategory;
import com.fesi.deadlinemate.domain.gathering.dto.request.GatheringSearchCondition;
import com.fesi.deadlinemate.domain.gathering.dto.response.GatheringDetailResponse;
import com.fesi.deadlinemate.domain.gathering.dto.response.GatheringListItemResponse;
import com.fesi.deadlinemate.domain.gathering.dto.response.GatheringListResponse;
import com.fesi.deadlinemate.domain.gathering.dto.response.GatheringMainResponse;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringTag;
import com.fesi.deadlinemate.domain.gathering.entity.WeeklyPlan;
import com.fesi.deadlinemate.domain.gathering.entity.WeeklyPlanDetail;
import com.fesi.deadlinemate.domain.gathering.projection.GatheringDetailRow;
import com.fesi.deadlinemate.domain.gathering.projection.GatheringListRow;
import com.fesi.deadlinemate.domain.category.repository.CategoryRepository;
import com.fesi.deadlinemate.domain.category.repository.GatheringCategoryRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatheringQueryService {

    private final GatheringRepository gatheringRepository;
    private final GatheringTagRepository gatheringTagRepository;
    private final GatheringImageRepository gatheringImageRepository;
    private final GatheringCategoryRepository gatheringCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final WeeklyPlanRepository weeklyPlanRepository;
    private final WeeklyPlanDetailRepository weeklyPlanDetailRepository;
    private final GatheringMemberRepository gatheringMemberRepository;
    private final GatheringLikeRepository gatheringLikeRepository;
    private final UserClient userClient;

    public GatheringListResponse getGatherings(GatheringSearchCondition condition, int page, int limit) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.max(limit, 1);

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit);
        Page<GatheringListRow> result = gatheringRepository.search(condition, pageable);

        List<GatheringListItemResponse> items = toListItemResponses(result.getContent());

        return GatheringListResponse.of(
                items,
                result.getTotalElements(),
                result.getTotalPages(),
                validatedPage
        );
    }

    public GatheringMainResponse getMainGatherings(int limit) {
        int validatedLimit = Math.max(limit, 1);

        List<GatheringListRow> popularRows  = gatheringRepository.findMainPopular(validatedLimit);
        List<GatheringListRow> deadlineRows = gatheringRepository.findMainDeadline(validatedLimit);
        List<GatheringListRow> latestRows   = gatheringRepository.findMainLatest(validatedLimit);

        List<GatheringListRow> allRows = Stream.of(popularRows, deadlineRows, latestRows)
                .flatMap(List::stream)
                .toList();

        if (allRows.isEmpty()) {
            return GatheringMainResponse.of(List.of(), List.of(), List.of());
        }

        RelatedData data = fetchRelatedData(allRows);

        return GatheringMainResponse.of(
                assembleResponses(popularRows,  data),
                assembleResponses(deadlineRows, data),
                assembleResponses(latestRows,   data)
        );
    }

    public GatheringListResponse getMyLikedGatherings(Long userId, int page, int limit) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.max(limit, 1);

        List<Long> likedGatheringIds = gatheringLikeRepository.findGatheringIdsByUserId(userId);

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit);
        Page<GatheringListRow> result = gatheringRepository.findByIdIn(likedGatheringIds, pageable);

        List<GatheringListItemResponse> items = toListItemResponses(result.getContent());

        return GatheringListResponse.of(
                items,
                result.getTotalElements(),
                result.getTotalPages(),
                validatedPage
        );
    }

    public GatheringDetailResponse getGatheringDetail(Long gatheringId) {
        GatheringDetailRow row = gatheringRepository.findDetailRowById(gatheringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

        List<String> categories = findCategoryNamesByGatheringId(gatheringId);

        List<String> tags = gatheringTagRepository.findByGatheringIdOrderByIdAsc(gatheringId).stream()
                .map(GatheringTag::getTag)
                .toList();

        List<GatheringDetailResponse.ImageResponse> images = gatheringImageRepository
                .findByGatheringIdOrderByDisplayOrderAsc(gatheringId).stream()
                .map(image -> GatheringDetailResponse.ImageResponse.builder()
                        .url(image.getImageUrl())
                        .displayOrder(image.getDisplayOrder())
                        .build())
                .toList();

        List<WeeklyPlan> weeklyPlans = weeklyPlanRepository.findByGatheringIdOrderByWeekNumberAsc(gatheringId);

        List<Long> weeklyPlanIds = weeklyPlans.stream()
                .map(WeeklyPlan::getId)
                .toList();

        Map<Long, List<String>> detailMap = weeklyPlanDetailRepository
                .findByWeeklyPlanIdInOrderByWeeklyPlanIdAscDisplayOrderAsc(weeklyPlanIds)
                .stream()
                .collect(Collectors.groupingBy(
                        WeeklyPlanDetail::getWeeklyPlanId,
                        LinkedHashMap::new,
                        Collectors.mapping(WeeklyPlanDetail::getContent, Collectors.toList())
                ));

        List<GatheringDetailResponse.WeeklyPlanResponse> weeklyPlanResponses = weeklyPlans.stream()
                .map(plan -> GatheringDetailResponse.WeeklyPlanResponse.builder()
                        .week(plan.getWeekNumber())
                        .title(plan.getTitle())
                        .startDate(plan.getStartDate())
                        .endDate(plan.getEndDate())
                        .details(detailMap.getOrDefault(plan.getId(), List.of()))
                        .build())
                .toList();

        List<GatheringMember> members = gatheringMemberRepository
                .findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId);

        Map<Long, UserInfo> memberUserMap = loadUsers(
                members.stream().map(GatheringMember::getUserId).distinct().toList()
        );

        List<GatheringDetailResponse.MemberResponse> memberResponses = members.stream()
                .map(member -> {
                    UserInfo userInfo = memberUserMap.get(member.getUserId());
                    return GatheringDetailResponse.MemberResponse.builder()
                            .userId(member.getUserId())
                            .nickname(userInfo != null ? userInfo.getNickname() : null)
                            .profileImage(userInfo != null ? userInfo.getProfileImage() : null)
                            .role(member.getRole())
                            .build();
                })
                .toList();

        UserInfo leader = userClient.findById(row.leaderId());

        return GatheringDetailResponse.builder()
                .id(row.id())
                .type(row.type().getDisplayName())
                .categories(categories)
                .title(row.title())
                .shortDescription(row.shortDescription())
                .description(row.description())
                .tags(tags)
                .goal(row.goal())
                .maxMembers(row.maxMembers())
                .currentMembers(row.currentMembers())
                .recruitDeadline(row.recruitDeadline())
                .startDate(row.startDate())
                .endDate(row.endDate())
                .totalWeeks(row.totalWeeks())
                .images(images)
                .status(row.status())
                .leader(GatheringDetailResponse.LeaderResponse.builder()
                        .id(leader.getId())
                        .nickname(leader.getNickname())
                        .profileImage(leader.getProfileImage())
                        .build())
                .weeklyPlans(weeklyPlanResponses)
                .members(memberResponses)
                .build();
    }

    private record RelatedData(
            Map<Long, List<String>> tagsMap,
            Map<Long, List<String>> imageUrlsMap,
            Map<Long, List<String>> categoryMap,
            Map<Long, UserInfo> leaderMap
    ) {}

    private RelatedData fetchRelatedData(List<GatheringListRow> rows) {
        List<Long> gatheringIds = rows.stream()
                .map(GatheringListRow::id)
                .distinct()
                .toList();

        Map<Long, List<String>> tagsMap = gatheringTagRepository
                .findByGatheringIdInOrderByGatheringIdAscIdAsc(gatheringIds)
                .stream()
                .collect(Collectors.groupingBy(
                        GatheringTag::getGatheringId,
                        LinkedHashMap::new,
                        Collectors.mapping(GatheringTag::getTag, Collectors.toList())
                ));

        Map<Long, List<String>> imageUrlsMap = gatheringImageRepository
                .findByGatheringIdInOrderByGatheringIdAscDisplayOrderAsc(gatheringIds)
                .stream()
                .collect(Collectors.groupingBy(
                        image -> image.getGatheringId(),
                        LinkedHashMap::new,
                        Collectors.mapping(image -> image.getImageUrl(), Collectors.toList())
                ));

        Map<Long, List<String>> categoryMap = buildCategoryMap(gatheringIds);

        Map<Long, UserInfo> leaderMap = loadUsers(
                rows.stream().map(GatheringListRow::leaderId).distinct().toList()
        );

        return new RelatedData(tagsMap, imageUrlsMap, categoryMap, leaderMap);
    }

    private List<GatheringListItemResponse> assembleResponses(
            List<GatheringListRow> rows, RelatedData data) {
        return rows.stream()
                .map(row -> {
                    UserInfo leader = data.leaderMap().get(row.leaderId());

                    return GatheringListItemResponse.builder()
                            .id(row.id())
                            .type(row.type().getDisplayName())
                            .categories(data.categoryMap().getOrDefault(row.id(), List.of()))
                            .title(row.title())
                            .shortDescription(row.shortDescription())
                            .imageUrls(data.imageUrlsMap().getOrDefault(row.id(), List.of()))
                            .tags(data.tagsMap().getOrDefault(row.id(), List.of()))
                            .maxMembers(row.maxMembers())
                            .currentMembers(row.currentMembers())
                            .recruitDeadline(row.recruitDeadline())
                            .startDate(row.startDate())
                            .endDate(row.endDate())
                            .status(row.status())
                            .leader(GatheringListItemResponse.LeaderSummary.builder()
                                    .id(leader != null ? leader.getId() : null)
                                    .nickname(leader != null ? leader.getNickname() : null)
                                    .profileImage(leader != null ? leader.getProfileImage() : null)
                                    .build())
                            .build();
                })
                .toList();
    }

    private List<GatheringListItemResponse> toListItemResponses(List<GatheringListRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        return assembleResponses(rows, fetchRelatedData(rows));
    }

    private Map<Long, List<String>> buildCategoryMap(List<Long> gatheringIds) {
        List<GatheringCategory> mappings = gatheringCategoryRepository.findByGatheringIdIn(gatheringIds);

        List<Long> categoryIds = mappings.stream()
                .map(GatheringCategory::getCategoryId)
                .distinct()
                .toList();

        Map<Long, String> categoryNameMap = categoryRepository.findByIdIn(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return mappings.stream()
                .collect(Collectors.groupingBy(
                        GatheringCategory::getGatheringId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                mapping -> categoryNameMap.get(mapping.getCategoryId()),
                                Collectors.filtering(Objects::nonNull, Collectors.toList())
                        )
                ));
    }

    private List<String> findCategoryNamesByGatheringId(Long gatheringId) {
        List<Long> categoryIds = gatheringCategoryRepository.findByGatheringId(gatheringId).stream()
                .map(GatheringCategory::getCategoryId)
                .toList();

        if (categoryIds.isEmpty()) {
            return List.of();
        }

        Map<Long, String> categoryNameMap = categoryRepository.findByIdIn(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return categoryIds.stream()
                .map(categoryNameMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, UserInfo> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        return userClient.findByIds(userIds);
    }
}
