package com.fesi.deadlinemate.domain.like.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.like.entity.GatheringLike;
import com.fesi.deadlinemate.domain.like.repository.GatheringLikeRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class GatheringLikeServiceTest {

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private GatheringLikeRepository gatheringLikeRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private GatheringLikeService gatheringLikeService;

    @Test
    @DisplayName("모임을 찜하면 찜 정보를 저장한다")
    void like_success() {
        // given
        Long gatheringId = 1L;
        Long userId = 100L;

        when(userClient.existsById(userId)).thenReturn(true);
        when(gatheringRepository.existsById(gatheringId)).thenReturn(true);

        // when
        gatheringLikeService.like(gatheringId, userId);

        // then
        ArgumentCaptor<GatheringLike> likeCaptor = ArgumentCaptor.forClass(GatheringLike.class);
        verify(gatheringLikeRepository).saveAndFlush(likeCaptor.capture());

        GatheringLike saved = likeCaptor.getValue();
        assertThat(saved.getGatheringId()).isEqualTo(gatheringId);
        assertThat(saved.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("이미 찜한 모임이면 예외가 발생한다")
    void like_fail_whenAlreadyLiked() {
        // given
        Long gatheringId = 1L;
        Long userId = 100L;

        when(userClient.existsById(userId)).thenReturn(true);
        when(gatheringRepository.existsById(gatheringId)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(gatheringLikeRepository)
                .saveAndFlush(any(GatheringLike.class));

        // when & then
        assertThatThrownBy(() -> gatheringLikeService.like(gatheringId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_GATHERING_LIKED);
    }

    @Test
    @DisplayName("찜한 모임을 취소하면 찜 정보를 삭제한다")
    void unlike_success() {
        // given
        Long gatheringId = 1L;
        Long userId = 100L;

        GatheringLike gatheringLike = GatheringLike.builder()
                .gatheringId(gatheringId)
                .userId(userId)
                .build();

        when(userClient.existsById(userId)).thenReturn(true);
        when(gatheringRepository.existsById(gatheringId)).thenReturn(true);
        when(gatheringLikeRepository.findByGatheringIdAndUserId(gatheringId, userId))
                .thenReturn(Optional.of(gatheringLike));

        // when
        gatheringLikeService.unlike(gatheringId, userId);

        // then
        verify(gatheringLikeRepository).delete(gatheringLike);
    }

    @Test
    @DisplayName("찜한 이력이 없으면 찜 취소 시 예외가 발생한다")
    void unlike_fail_whenLikeNotFound() {
        // given
        Long gatheringId = 1L;
        Long userId = 100L;

        when(userClient.existsById(userId)).thenReturn(true);
        when(gatheringRepository.existsById(gatheringId)).thenReturn(true);
        when(gatheringLikeRepository.findByGatheringIdAndUserId(gatheringId, userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> gatheringLikeService.unlike(gatheringId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.GATHERING_LIKE_NOT_FOUND);

        verify(gatheringLikeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("동시 찜 취소로 이미 삭제된 찜을 다시 지우려 하면 찜 없음 예외로 변환한다")
    void unlike_fail_whenAlreadyDeletedConcurrently() {
        // given
        Long gatheringId = 1L;
        Long userId = 100L;

        GatheringLike gatheringLike = GatheringLike.builder()
                .gatheringId(gatheringId)
                .userId(userId)
                .build();

        when(userClient.existsById(userId)).thenReturn(true);
        when(gatheringRepository.existsById(gatheringId)).thenReturn(true);
        when(gatheringLikeRepository.findByGatheringIdAndUserId(gatheringId, userId))
                .thenReturn(Optional.of(gatheringLike));
        doThrow(new ObjectOptimisticLockingFailureException(GatheringLike.class, 1L))
                .when(gatheringLikeRepository)
                .flush();

        // when & then
        assertThatThrownBy(() -> gatheringLikeService.unlike(gatheringId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.GATHERING_LIKE_NOT_FOUND);
    }
}
