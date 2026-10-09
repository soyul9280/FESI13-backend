package com.fesi.deadlinemate.domain.gatheringApplication.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.gatheringApplication.entity.ApplicationStatus;
import com.fesi.deadlinemate.domain.gatheringApplication.entity.GatheringApplication;
import com.fesi.deadlinemate.domain.gatheringApplication.repository.GatheringApplicationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * GatheringApplicationConcurrencyTest의 대조군. 서비스를 거치지 않고
 * findByIdForUpdate() 대신 락 없는 findById()로 같은 수락 로직을 직접 재현해,
 * PESSIMISTIC_WRITE가 실제로 막고 있는 정원 초과가 락 없이는 재현되는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class GatheringCapacityRaceWithoutLockTest {

    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private GatheringApplicationRepository applicationRepository;
    @Autowired private GatheringMemberRepository memberRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private static final Long LEADER_ID   = 300L;
    private static final Long APPLICANT_A = 301L;
    private static final Long APPLICANT_B = 302L;

    private Gathering gathering;
    private GatheringApplication appA;
    private GatheringApplication appB;

    @BeforeEach
    void setUp() {
        gathering = gatheringRepository.save(Gathering.builder()
                .leaderId(LEADER_ID)
                .type(GatheringType.STUDY)
                .title("락 없는 수락 재현")
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

        appA = applicationRepository.save(GatheringApplication.builder()
                .gatheringId(gathering.getId())
                .applicantId(APPLICANT_A)
                .personalGoal("열심히 하겠습니다")
                .status(ApplicationStatus.PENDING)
                .build());

        appB = applicationRepository.save(GatheringApplication.builder()
                .gatheringId(gathering.getId())
                .applicantId(APPLICANT_B)
                .personalGoal("열심히 하겠습니다")
                .status(ApplicationStatus.PENDING)
                .build());
    }

    @AfterEach
    void cleanup() {
        applicationRepository.deleteAll();
        memberRepository.deleteAll();
        gatheringRepository.deleteAll();
    }

    @Test
    @DisplayName("[대조군] 락 없는 findById로 동시에 수락을 처리하면 정원이 초과된다")
    void 락_없이_수락하면_정원이_초과된다() throws InterruptedException {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CyclicBarrier bothValidated = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch doneLatch = new CountDownLatch(2);

        executor.submit(() -> acceptWithoutLock(transactionTemplate, bothValidated, doneLatch,
                appA.getId(), APPLICANT_A));
        executor.submit(() -> acceptWithoutLock(transactionTemplate, bothValidated, doneLatch,
                appB.getId(), APPLICANT_B));

        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).as("두 스레드가 10초 내에 완료되어야 합니다").isTrue();

        long activeMemberCount = memberRepository
                .findByGatheringIdAndIsActiveTrueOrderByIdAsc(gathering.getId())
                .size();
        Gathering refreshed = gatheringRepository.findById(gathering.getId()).orElseThrow();

        // 락 없이 두 트랜잭션이 모두 "자리 있음"을 통과한 뒤 각자 멤버를 추가해,
        // 실제 활성 멤버 수(3명)가 정원(2명)을 넘는다 — PESSIMISTIC_WRITE가 막던 문제가 그대로 재현된다.
        assertThat(activeMemberCount)
                .as("락이 없으면 실제 활성 멤버 수가 정원을 초과한다")
                .isGreaterThan(gathering.getMaxMembers());
        // currentMembers 필드는 두 트랜잭션이 똑같이 "1에서 읽어 2로 저장"하는 lost update를 겪어,
        // 실제 멤버 수(3명)보다 더 적은 값으로 DB에 남는다 — 카운터와 실제 데이터가 어긋난다.
        assertThat(refreshed.getCurrentMembers())
                .as("currentMembers는 lost update로 실제 멤버 수보다 작게 남는다")
                .isLessThan((int) activeMemberCount);
    }

    private void acceptWithoutLock(
            TransactionTemplate transactionTemplate,
            CyclicBarrier bothValidated,
            CountDownLatch doneLatch,
            Long applicationId,
            Long applicantId
    ) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                // findByIdForUpdate() 대신, 프로덕션 수정 전과 동일한 락 없는 조회를 그대로 사용한다.
                Gathering g = gatheringRepository.findById(gathering.getId()).orElseThrow();
                g.validateCapacity();

                try {
                    bothValidated.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                } catch (java.util.concurrent.TimeoutException e) {
                    throw new RuntimeException(e);
                }

                GatheringApplication application = applicationRepository.findById(applicationId).orElseThrow();
                application.accept();

                GatheringMember member = GatheringMember.builder()
                        .gatheringId(g.getId())
                        .userId(applicantId)
                        .role(GatheringRole.MEMBER)
                        .personalGoal(application.getPersonalGoal())
                        .isActive(true)
                        .overallAchievementRate(BigDecimal.ZERO)
                        .build();
                memberRepository.save(member);

                g.increaseCurrentMembers();
                gatheringRepository.save(g);
            });
        } finally {
            doneLatch.countDown();
        }
    }
}