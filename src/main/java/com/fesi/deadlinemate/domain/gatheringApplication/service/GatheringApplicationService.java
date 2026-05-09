package com.fesi.deadlinemate.domain.gatheringApplication.service;

import com.fesi.deadlinemate.domain.gathering.dto.response.MyApplicationStatusResponse;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.gatheringApplication.command.CreateApplicationCommand;
import com.fesi.deadlinemate.domain.gatheringApplication.command.UpdateApplicationCommand;
import com.fesi.deadlinemate.domain.gatheringApplication.dto.response.ApplicationListResponse;
import com.fesi.deadlinemate.domain.gatheringApplication.dto.response.CreateApplicationResponse;
import com.fesi.deadlinemate.domain.gatheringApplication.dto.response.MyApplicationListResponse;
import com.fesi.deadlinemate.domain.gatheringApplication.dto.response.UpdateApplicationResponse;
import com.fesi.deadlinemate.domain.gatheringApplication.entity.ApplicationStatus;
import com.fesi.deadlinemate.domain.gatheringApplication.entity.GatheringApplication;
import com.fesi.deadlinemate.domain.gatheringApplication.event.GatheringApplicationCancelledEvent;
import com.fesi.deadlinemate.domain.gatheringApplication.event.GatheringApplicationCreatedEvent;
import com.fesi.deadlinemate.domain.gatheringApplication.event.GatheringApplicationUpdatedEvent;
import com.fesi.deadlinemate.domain.gatheringApplication.repository.GatheringApplicationRepository;
import com.fesi.deadlinemate.domain.review.client.ReviewClient;
import com.fesi.deadlinemate.domain.review.client.dto.ApplicantReviewInfo;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatheringApplicationService {

    private final GatheringRepository gatheringRepository;
    private final GatheringApplicationRepository gatheringApplicationRepository;
    private final GatheringMemberRepository gatheringMemberRepository;
    private final UserClient userClient;
    private final ReviewClient reviewClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreateApplicationResponse create(CreateApplicationCommand command) {
        validateApplicantExists(command.applicantId());

        Gathering gathering = gatheringRepository.findById(command.gatheringId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

        validateApplicable(gathering, command.applicantId());

        String personalGoal = command.personalGoal().trim();
        String selfIntroduction = normalizeNullableText(command.selfIntroduction());

        GatheringApplication application = GatheringApplication.builder()
                .gatheringId(gathering.getId())
                .applicantId(command.applicantId())
                .personalGoal(personalGoal)
                .selfIntroduction(selfIntroduction)
                .status(ApplicationStatus.PENDING)
                .build();

        GatheringApplication saved;
        try {
            saved = gatheringApplicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_GATHERING_APPLICATION);
        }

        eventPublisher.publishEvent(new GatheringApplicationCreatedEvent(
                saved.getId(),
                saved.getGatheringId(),
                saved.getApplicantId(),
                gathering.getLeaderId(),
                gathering.getTitle()
        ));

        return CreateApplicationResponse.from(saved);
    }

    public ApplicationListResponse getApplications(Long gatheringId, Long requesterId) {
        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

        gathering.validateLeader(requesterId);

        List<GatheringApplication> applications =
                gatheringApplicationRepository.findByGatheringIdOrderByCreatedAtAsc(gatheringId);

        List<Long> applicantIds = applications.stream()
                .map(GatheringApplication::getApplicantId)
                .distinct()
                .toList();

        Map<Long, UserInfo> applicantMap = userClient.findByIds(applicantIds);
        Map<Long, ApplicantReviewInfo> applicantReviewInfoMap =
                reviewClient.getApplicantReviewInfos(applicantIds);

        List<ApplicationListResponse.ApplicationItemResponse> responses = applications.stream()
                .map(application -> toApplicationItemResponse(
                        application,
                        applicantMap.get(application.getApplicantId()),
                        applicantReviewInfoMap.get(application.getApplicantId())
                ))
                .toList();

        return ApplicationListResponse.of(responses);
    }

    @Transactional
    public UpdateApplicationResponse updateApplication(UpdateApplicationCommand command) {
        validateUpdatableStatus(command.status());
        Gathering gathering = getGatheringForUpdate(command);
        gathering.validateLeader(command.requesterId());
        GatheringApplication application = getApplicationForUpdate(command);

        if (command.status() == ApplicationStatus.ACCEPTED) {
            acceptApplication(gathering, application);
        } else {
            rejectApplication(application);
        }

        eventPublisher.publishEvent(new GatheringApplicationUpdatedEvent(
                application.getId(),
                gathering.getId(),
                application.getApplicantId(),
                gathering.getLeaderId(),
                gathering.getTitle(),
                application.getStatus()
        ));

        return UpdateApplicationResponse.from(application);
    }

    @Transactional
    public void cancelApplication(Long gatheringId, Long applicationId, Long requesterId) {
        GatheringApplication application = gatheringApplicationRepository
                .findByIdAndGatheringId(applicationId, gatheringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

        application.validateCancelableBy(requesterId);

        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

        gatheringApplicationRepository.delete(application);

        eventPublisher.publishEvent(new GatheringApplicationCancelledEvent(
                application.getId(),
                gatheringId,
                application.getApplicantId(),
                gathering.getLeaderId(),
                gathering.getTitle()
        ));
    }

    public MyApplicationListResponse getMyApplications(Long applicantId) {
        validateApplicantExists(applicantId);

        List<GatheringApplication> applications =
                gatheringApplicationRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId);

        if (applications.isEmpty()) {
            return MyApplicationListResponse.of(List.of());
        }

        List<Long> gatheringIds = applications.stream()
                .map(GatheringApplication::getGatheringId)
                .distinct()
                .toList();

        Map<Long, Gathering> gatheringMap = gatheringRepository.findAllById(gatheringIds).stream()
                .collect(Collectors.toMap(Gathering::getId, Function.identity()));

        List<MyApplicationListResponse.MyApplicationItemResponse> responses = applications.stream()
                .map(application -> {
                    Gathering gathering = gatheringMap.get(application.getGatheringId());

                    if (gathering == null) {
                        return null;
                    }

                    return MyApplicationListResponse.MyApplicationItemResponse.builder()
                            .id(application.getId())
                            .gathering(MyApplicationListResponse.GatheringSummaryResponse.builder()
                                    .id(gathering.getId())
                                    .title(gathering.getTitle())
                                    .type(gathering.getType().getDisplayName())
                                    .status(gathering.getStatus())
                                    .build())
                            .personalGoal(application.getPersonalGoal())
                            .status(application.getStatus())
                            .createdAt(application.getCreatedAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        return MyApplicationListResponse.of(responses);
    }

    public MyApplicationStatusResponse getMyApplicationStatus(Long gatheringId, Long userId) {
        GatheringApplication application = gatheringApplicationRepository
                .findByGatheringIdAndApplicantId(gatheringId, userId)
                .orElse(null);

        return MyApplicationStatusResponse.of(
                application == null ? null : application.getStatus().name()
        );
    }

    private Gathering getGatheringForUpdate(UpdateApplicationCommand command) {
        if (command.status() == ApplicationStatus.ACCEPTED) {
            return gatheringRepository.findByIdForUpdate(command.gatheringId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));
        }

        return gatheringRepository.findById(command.gatheringId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));
    }

    private GatheringApplication getApplicationForUpdate(UpdateApplicationCommand command) {
        if (command.status() == ApplicationStatus.ACCEPTED) {
            return gatheringApplicationRepository
                    .findByIdAndGatheringIdForUpdate(command.applicationId(), command.gatheringId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        }

        return gatheringApplicationRepository.findByIdAndGatheringId(command.applicationId(), command.gatheringId())
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }


    private void acceptApplication(Gathering gathering, GatheringApplication application) {
        gathering.validateCapacity();

        boolean alreadyMember = gatheringMemberRepository
                .existsByGatheringIdAndUserIdAndIsActiveTrue(gathering.getId(), application.getApplicantId());

        if (alreadyMember) {
            throw new BusinessException(ErrorCode.ALREADY_GATHERING_MEMBER);
        }

        application.accept();

        GatheringMember member = GatheringMember.builder()
                .gatheringId(gathering.getId())
                .userId(application.getApplicantId())
                .role(GatheringRole.MEMBER)
                .personalGoal(application.getPersonalGoal())
                .isActive(true)
                .overallAchievementRate(BigDecimal.ZERO)
                .build();

        try {
            gatheringMemberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_GATHERING_MEMBER);
        }
        gathering.increaseCurrentMembers();
    }

    private void rejectApplication(GatheringApplication application) {
        application.reject();
    }

    private void validateApplicantExists(Long applicantId) {
        if (applicantId == null || !userClient.existsById(applicantId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateApplicable(Gathering gathering, Long applicantId) {
        if (gathering.getLeaderId().equals(applicantId)) {
            throw new BusinessException(ErrorCode.GATHERING_LEADER_CANNOT_APPLY);
        }

        gathering.validateRecruiting();
        gathering.validateCapacity();

        if (gatheringApplicationRepository.existsByGatheringIdAndApplicantId(gathering.getId(), applicantId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_GATHERING_APPLICATION);
        }

        boolean alreadyMember = gatheringMemberRepository
                .existsByGatheringIdAndUserIdAndIsActiveTrue(gathering.getId(), applicantId);

        if (alreadyMember) {
            throw new BusinessException(ErrorCode.ALREADY_GATHERING_MEMBER);
        }
    }

    private void validateUpdatableStatus(ApplicationStatus status) {
        if (status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS_CHANGE);
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private ApplicationListResponse.ApplicationItemResponse toApplicationItemResponse(
            GatheringApplication application,
            UserInfo applicant,
            ApplicantReviewInfo reviewInfo
    ) {
        if (applicant == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        ApplicationListResponse.ReviewSummaryResponse reviewSummary =
                ApplicationListResponse.ReviewSummaryResponse.builder()
                        .reviewCount(reviewInfo == null ? 0 : reviewInfo.reviewCount())
                        .topTags(reviewInfo == null ? List.of() : reviewInfo.topTags())
                        .build();

        List<ApplicationListResponse.RecentReviewResponse> recentReviews =
                reviewInfo == null ? List.of() : reviewInfo.recentReviews().stream()
                        .map(review -> ApplicationListResponse.RecentReviewResponse.builder()
                                .id(review.id())
                                .comment(review.comment())
                                .tags(review.tags())
                                .build())
                        .toList();

        return ApplicationListResponse.ApplicationItemResponse.builder()
                .id(application.getId())
                .applicant(ApplicationListResponse.ApplicantResponse.builder()
                        .id(applicant.getId())
                        .nickname(applicant.getNickname())
                        .profileImage(applicant.getProfileImage())
                        .reputationScore(applicant.getReputationScore())
                        .reviewSummary(reviewSummary)
                        .recentReviews(recentReviews)
                        .build())
                .personalGoal(application.getPersonalGoal())
                .selfIntroduction(application.getSelfIntroduction())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }

}