package com.fesi.deadlinemate.domain.like.service;

import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import java.util.List;
import com.fesi.deadlinemate.domain.like.entity.GatheringLike;
import com.fesi.deadlinemate.domain.like.repository.GatheringLikeRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatheringLikeService {

    private final GatheringRepository gatheringRepository;
    private final GatheringLikeRepository gatheringLikeRepository;
    private final UserClient userClient;

    public List<Long> getLikedGatheringIds(Long userId) {
        return gatheringLikeRepository.findGatheringIdsByUserId(userId);
    }

    @Transactional
    public void like(Long gatheringId, Long userId) {
        validateUserExists(userId);
        validateGatheringExists(gatheringId);

        GatheringLike gatheringLike = GatheringLike.builder()
                .gatheringId(gatheringId)
                .userId(userId)
                .build();

        try {
            gatheringLikeRepository.saveAndFlush(gatheringLike);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_GATHERING_LIKED);
        }
    }

    @Transactional
    public void unlike(Long gatheringId, Long userId) {
        validateUserExists(userId);
        validateGatheringExists(gatheringId);

        GatheringLike gatheringLike = gatheringLikeRepository.findByGatheringIdAndUserId(gatheringId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_LIKE_NOT_FOUND));

        try {
            gatheringLikeRepository.delete(gatheringLike);
            gatheringLikeRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ErrorCode.GATHERING_LIKE_NOT_FOUND);
        }
    }

    private void validateUserExists(Long userId) {
        if (userId == null || !userClient.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateGatheringExists(Long gatheringId) {
        boolean exists = gatheringRepository.existsById(gatheringId);
        if (!exists) {
            throw new BusinessException(ErrorCode.GATHERING_NOT_FOUND);
        }
    }
}
