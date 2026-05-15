package com.fesi.deadlinemate.domain.gathering.service;

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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipQueryService {

    private final GatheringMemberRepository gatheringMemberRepository;
    private final GatheringRepository gatheringRepository;
    private final GatheringTagRepository gatheringTagRepository;
    private final GatheringApplicationRepository gatheringApplicationRepository;
    private final ReviewRepository reviewRepository;
    private final GatheringCategoryRepository gatheringCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;

    public MyGatheringListResponse getMyGatherings(Long userId, String status, String sort, int page, int limit) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.max(limit, 1);

        List<Long> gatheringIds = gatheringMemberRepository.findActiveGatheringIdsByUserId(userId);

        if (gatheringIds.isEmpty()) {
            return MyGatheringListResponse.builder()
                    .gatherings(List.of())
                    .totalCount(0)
                    .totalPages(0)
                    .currentPage(validatedPage)
                    .build();
        }

        boolean isOldest = "oldest".equalsIgnoreCase(sort);
        PageRequest pageable = PageRequest.of(validatedPage - 1, validatedLimit);
        Page<Gathering> result = resolveGatheringPage(gatheringIds, status, isOldest, pageable);

        List<Long> resultGatheringIds = result.getContent().stream()
                .map(Gathering::getId).toList();

        Map<Long, GatheringMember> memberMap = gatheringMemberRepository
                .findByGatheringIdInAndUserIdAndIsActiveTrue(resultGatheringIds, userId).stream()
                .collect(Collectors.toMap(GatheringMember::getGatheringId, m -> m));

        Map<Long, List<String>> tagsMap = gatheringTagRepository
                .findTagRowsByGatheringIdIn(resultGatheringIds).stream()
                .collect(Collectors.groupingBy(
                        row -> row.getGatheringId(),
                        LinkedHashMap::new,
                        Collectors.mapping(row -> row.getTag(), Collectors.toList())
                ));

        Map<Long, List<String>> categoriesMap = buildCategoriesMap(resultGatheringIds);

        Map<Long, Integer> reviewedCountMap = reviewRepository
                .countReviewedMembersGroupByGathering(userId, resultGatheringIds)
                .stream()
                .collect(Collectors.toMap(
                        ReviewRepository.ReviewCountRow::getGatheringId,
                        row -> row.getCount().intValue()
                ));

        List<Long> leaderGatheringIds = memberMap.values().stream()
                .filter(m -> GatheringRole.LEADER == m.getRole())
                .map(GatheringMember::getGatheringId)
                .toList();

        Map<Long, Integer> pendingCountMap = gatheringApplicationRepository
                .countByGatheringIdInAndStatus(leaderGatheringIds, ApplicationStatus.PENDING)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        List<MyGatheringListResponse.MyGatheringItem> items = result.getContent().stream()
                .map(gathering -> {
                    GatheringMember member = memberMap.get(gathering.getId());
                    List<String> categories = categoriesMap.getOrDefault(gathering.getId(), List.of());
                    List<String> tags = tagsMap.getOrDefault(gathering.getId(), List.of());
                    Integer pendingCount = GatheringRole.LEADER == (member != null ? member.getRole() : null)
                            ? pendingCountMap.getOrDefault(gathering.getId(), 0)
                            : null;
                    int reviewedCount = reviewedCountMap.getOrDefault(gathering.getId(), 0);
                    return MyGatheringListResponse.MyGatheringItem.of(
                            gathering,
                            member != null ? member.getRole() : null,
                            categories,
                            tags,
                            reviewedCount > 0,
                            reviewedCount,
                            pendingCount
                    );
                })
                .toList();

        return MyGatheringListResponse.builder()
                .gatherings(items)
                .totalCount(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(validatedPage)
                .build();
    }

    public MemberListResponse getMembers(Long gatheringId, Long requesterId) {
        validateMembership(gatheringId, requesterId);

        List<GatheringMember> members = gatheringMemberRepository
                .findByGatheringIdAndIsActiveTrueOrderByIdAsc(gatheringId);

        List<Long> userIds = members.stream().map(GatheringMember::getUserId).distinct().toList();
        Map<Long, UserInfo> userMap = userClient.findByIds(userIds);

        Map<Long, BigDecimal> achievementRates = members.stream()
                .collect(Collectors.toMap(
                        GatheringMember::getUserId,
                        GatheringMember::getOverallAchievementRate
                ));

        return MemberListResponse.of(members, userMap, achievementRates);
    }

    private Page<Gathering> resolveGatheringPage(List<Long> ids, String status, boolean isOldest, PageRequest pageable) {
        GatheringStatus gatheringStatus = GatheringStatus.fromString(status);
        if (gatheringStatus != null) {
            return isOldest
                    ? gatheringRepository.findByIdInAndStatusOrderByCreatedAtAsc(ids, gatheringStatus, pageable)
                    : gatheringRepository.findByIdInAndStatusOrderByCreatedAtDesc(ids, gatheringStatus, pageable);
        }
        return isOldest
                ? gatheringRepository.findByIdInOrderByCreatedAtAsc(ids, pageable)
                : gatheringRepository.findByIdInOrderByCreatedAtDesc(ids, pageable);
    }

    private void validateMembership(Long gatheringId, Long userId) {
        if (!gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(gatheringId, userId)) {
            throw new BusinessException(ErrorCode.NOT_A_MEMBER);
        }
    }

    private Map<Long, List<String>> buildCategoriesMap(List<Long> gatheringIds) {
        List<GatheringCategory> mappings = gatheringCategoryRepository.findByGatheringIdIn(gatheringIds);

        if (mappings.isEmpty()) {
            return Map.of();
        }

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
}