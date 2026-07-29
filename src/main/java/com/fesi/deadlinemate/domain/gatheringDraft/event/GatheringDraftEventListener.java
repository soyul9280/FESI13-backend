package com.fesi.deadlinemate.domain.gatheringDraft.event;

import com.fesi.deadlinemate.domain.gathering.event.GatheringCreatedEvent;
import com.fesi.deadlinemate.domain.gatheringDraft.service.GatheringDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GatheringDraftEventListener {

    private final GatheringDraftService gatheringDraftService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGatheringCreated(GatheringCreatedEvent event) {
        if (event.draftId() == null) {
            return;
        }
        gatheringDraftService.deleteQuietly(event.draftId(), event.leaderId());
    }
}