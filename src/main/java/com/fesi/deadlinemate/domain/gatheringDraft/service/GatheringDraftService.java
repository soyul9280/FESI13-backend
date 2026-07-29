package com.fesi.deadlinemate.domain.gatheringDraft.service;

import com.fesi.deadlinemate.domain.gatheringDraft.command.CreateGatheringDraftCommand;
import com.fesi.deadlinemate.domain.gatheringDraft.command.UpdateGatheringDraftCommand;
import com.fesi.deadlinemate.domain.gatheringDraft.dto.response.GatheringDraftSaveResponse;
import com.fesi.deadlinemate.domain.gatheringDraft.entity.GatheringDraft;
import com.fesi.deadlinemate.domain.gatheringDraft.repository.GatheringDraftRepository;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GatheringDraftService {

    private static final int MAX_DRAFT_COUNT = 5;

    private final GatheringDraftRepository gatheringDraftRepository;

    public GatheringDraftSaveResponse create(CreateGatheringDraftCommand command) {
        List<GatheringDraft> existingDrafts = gatheringDraftRepository.findByUserIdForUpdate(command.userId());
        if (existingDrafts.size() >= MAX_DRAFT_COUNT) {
            throw new BusinessException(ErrorCode.GATHERING_DRAFT_LIMIT_EXCEEDED);
        }

        GatheringDraft draft = GatheringDraft.builder()
                .userId(command.userId())
                .type(command.type())
                .categoryIds(command.categoryIds())
                .title(command.title())
                .shortDescription(command.shortDescription())
                .description(command.description())
                .tags(command.tags())
                .goal(command.goal())
                .maxMembers(command.maxMembers())
                .recruitDeadline(command.recruitDeadline())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .weeklyGuides(command.weeklyGuides())
                .build();

        GatheringDraft saved = gatheringDraftRepository.save(draft);
        return GatheringDraftSaveResponse.from(saved);
    }

    public GatheringDraftSaveResponse update(Long draftId, UpdateGatheringDraftCommand command) {
        GatheringDraft draft = findDraft(draftId);
        draft.validateOwner(command.userId());

        draft.update(
                command.type(),
                command.categoryIds(),
                command.title(),
                command.shortDescription(),
                command.description(),
                command.tags(),
                command.goal(),
                command.maxMembers(),
                command.recruitDeadline(),
                command.startDate(),
                command.endDate(),
                command.weeklyGuides()
        );

        gatheringDraftRepository.flush();
        return GatheringDraftSaveResponse.from(draft);
    }

    public void delete(Long draftId, Long userId) {
        GatheringDraft draft = findDraft(draftId);
        draft.validateOwner(userId);

        gatheringDraftRepository.delete(draft);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteQuietly(Long draftId, Long userId) {
        gatheringDraftRepository.findById(draftId)
                .filter(draft -> draft.getUserId().equals(userId))
                .ifPresent(gatheringDraftRepository::delete);
    }

    private GatheringDraft findDraft(Long draftId) {
        return gatheringDraftRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_DRAFT_NOT_FOUND));
    }
}