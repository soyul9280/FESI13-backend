package com.fesi.deadlinemate.domain.gatheringDraft.service;

import com.fesi.deadlinemate.domain.gatheringDraft.dto.response.GatheringDraftDetailResponse;
import com.fesi.deadlinemate.domain.gatheringDraft.dto.response.GatheringDraftListResponse;
import com.fesi.deadlinemate.domain.gatheringDraft.entity.GatheringDraft;
import com.fesi.deadlinemate.domain.gatheringDraft.repository.GatheringDraftRepository;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatheringDraftQueryService {

    private final GatheringDraftRepository gatheringDraftRepository;

    public GatheringDraftListResponse getMyDrafts(Long userId) {
        return GatheringDraftListResponse.from(
                gatheringDraftRepository.findByUserIdOrderByUpdatedAtDesc(userId)
        );
    }

    public GatheringDraftDetailResponse getDraftDetail(Long draftId, Long userId) {
        GatheringDraft draft = gatheringDraftRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_DRAFT_NOT_FOUND));
        draft.validateOwner(userId);

        return GatheringDraftDetailResponse.from(draft);
    }
}