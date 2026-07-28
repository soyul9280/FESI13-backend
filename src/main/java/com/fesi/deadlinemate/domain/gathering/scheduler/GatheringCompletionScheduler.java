package com.fesi.deadlinemate.domain.gathering.scheduler;

import com.fesi.deadlinemate.domain.gathering.service.GatheringService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatheringCompletionScheduler {

    private final GatheringService gatheringService;
    private final Clock clock;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void startBegunGatherings() {
        gatheringService.startBegunGatherings(LocalDate.now(clock));
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void completeEndedGatherings() {
        gatheringService.completeEndedGatherings(LocalDate.now(clock));
    }
}
