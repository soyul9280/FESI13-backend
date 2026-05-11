package com.fesi.deadlinemate.domain.gatheringApplication.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.gatheringApplication.command.UpdateApplicationCommand;
import com.fesi.deadlinemate.domain.gatheringApplication.entity.ApplicationStatus;
import com.fesi.deadlinemate.domain.gatheringApplication.entity.GatheringApplication;
import com.fesi.deadlinemate.domain.gatheringApplication.repository.GatheringApplicationRepository;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GatheringApplicationConcurrencyTest {

    @Autowired private GatheringApplicationService service;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private GatheringApplicationRepository applicationRepository;
    @Autowired private GatheringMemberRepository memberRepository;

    private static final Long LEADER_ID   = 100L;
    private static final Long APPLICANT_A = 101L;
    private static final Long APPLICANT_B = 102L;

    private Gathering gathering;

    @BeforeEach
    void setUp() {
        gathering = gatheringRepository.save(Gathering.builder()
                .leaderId(LEADER_ID)
                .type(GatheringType.STUDY)
                .title("동시성 테스트 스터디")
                .shortDescription("짧은 소개")
                .description("상세 설명")
                .goal("목표")
                .maxMembers(2)
                .currentMembers(1)
                .recruitDeadline(LocalDate.of(2099, 12, 1))
                .startDate(LocalDate.of(2099, 12, 15))
                .endDate(LocalDate.of(2100, 3, 15))
                .totalWeeks(13)
                .status(GatheringStatus.RECRUITING)
                .viewCount(0)
                .build());

        memberRepository.save(GatheringMember.builder()
                .gatheringId(gathering.getId())
                .userId(LEADER_ID)
                .role(GatheringRole.LEADER)
                .isActive(true)
                .overallAchievementRate(BigDecimal.ZERO)
                .build());
    }

    @AfterEach
    void cleanup() {
        applicationRepository.deleteAll();
        memberRepository.deleteAll();
        gatheringRepository.deleteAll();
    }

    @Test
    @DisplayName("정원 1명 남은 모임에 동시 수락 시도 시 한 명만 성공한다")
    void 동시_수락_시_정원_초과_방지() throws InterruptedException {
        GatheringApplication appA = applicationRepository.save(GatheringApplication.builder()
                .gatheringId(gathering.getId())
                .applicantId(APPLICANT_A)
                .personalGoal("열심히 하겠습니다")
                .status(ApplicationStatus.PENDING)
                .build());

        GatheringApplication appB = applicationRepository.save(GatheringApplication.builder()
                .gatheringId(gathering.getId())
                .applicantId(APPLICANT_B)
                .personalGoal("열심히 하겠습니다")
                .status(ApplicationStatus.PENDING)
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger fullCount    = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                service.updateApplication(UpdateApplicationCommand.builder()
                        .gatheringId(gathering.getId())
                        .applicationId(appA.getId())
                        .requesterId(LEADER_ID)
                        .status(ApplicationStatus.ACCEPTED)
                        .build());
                successCount.incrementAndGet();
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.GATHERING_FULL) fullCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                service.updateApplication(UpdateApplicationCommand.builder()
                        .gatheringId(gathering.getId())
                        .applicationId(appB.getId())
                        .requesterId(LEADER_ID)
                        .status(ApplicationStatus.ACCEPTED)
                        .build());
                successCount.incrementAndGet();
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.GATHERING_FULL) fullCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).as("두 스레드가 5초 내에 완료되어야 합니다").isTrue();

        Gathering refreshed = gatheringRepository.findById(gathering.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(fullCount.get()).isEqualTo(1);
        assertThat(refreshed.getCurrentMembers()).isEqualTo(2);
        assertThat(memberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(gathering.getId())).hasSize(2);
    }

    @Test
    @DisplayName("수락으로 정원이 꽉 찼을 때 나머지 PENDING 신청은 자동으로 거절된다")
    void 수락_후_정원_마감_시_나머지_PENDING_자동_거절() {
        GatheringApplication appA = applicationRepository.save(GatheringApplication.builder()
                .gatheringId(gathering.getId())
                .applicantId(APPLICANT_A)
                .personalGoal("열심히 하겠습니다")
                .status(ApplicationStatus.PENDING)
                .build());

        GatheringApplication appB = applicationRepository.save(GatheringApplication.builder()
                .gatheringId(gathering.getId())
                .applicantId(APPLICANT_B)
                .personalGoal("열심히 하겠습니다")
                .status(ApplicationStatus.PENDING)
                .build());

        service.updateApplication(UpdateApplicationCommand.builder()
                .gatheringId(gathering.getId())
                .applicationId(appA.getId())
                .requesterId(LEADER_ID)
                .status(ApplicationStatus.ACCEPTED)
                .build());

        GatheringApplication refreshedA = applicationRepository.findById(appA.getId()).orElseThrow();
        GatheringApplication refreshedB = applicationRepository.findById(appB.getId()).orElseThrow();
        Gathering refreshed = gatheringRepository.findById(gathering.getId()).orElseThrow();

        assertThat(refreshedA.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(refreshedB.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(refreshed.getCurrentMembers()).isEqualTo(2);
    }
}
