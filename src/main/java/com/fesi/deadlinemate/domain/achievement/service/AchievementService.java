package com.fesi.deadlinemate.domain.achievement.service;

import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AchievementService {

    private final GatheringMemberRepository gatheringMemberRepository;
    private final TodoRepository todoRepository;

    public void sync(Long gatheringId, Long userId) {
        GatheringMember member = gatheringMemberRepository
                .findByGatheringIdAndUserIdAndIsActiveTrue(gatheringId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        long totalCount = todoRepository.countByGatheringIdAndUserId(gatheringId, userId);
        long completedCount = todoRepository.countByGatheringIdAndUserIdAndIsCompletedTrue(gatheringId, userId);

        BigDecimal rate = totalCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);

        member.updateOverallAchievementRate(rate);
    }
}
