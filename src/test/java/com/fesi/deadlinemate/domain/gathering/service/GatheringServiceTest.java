package com.fesi.deadlinemate.domain.gathering.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.fesi.deadlinemate.domain.category.entity.Category;
import com.fesi.deadlinemate.domain.category.entity.GatheringCategory;
import com.fesi.deadlinemate.domain.category.repository.CategoryRepository;
import com.fesi.deadlinemate.domain.category.repository.GatheringCategoryRepository;
import com.fesi.deadlinemate.domain.gathering.command.CreateGatheringCommand;
import com.fesi.deadlinemate.domain.gathering.command.UpdateGatheringCommand;
import com.fesi.deadlinemate.domain.gathering.dto.response.CreateGatheringResponse;
import com.fesi.deadlinemate.domain.gathering.dto.response.UpdateGatheringResponse;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringRole;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringStatus;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringTag;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringType;
import com.fesi.deadlinemate.domain.gathering.entity.WeeklyPlan;
import com.fesi.deadlinemate.domain.gathering.entity.WeeklyPlanDetail;
import com.fesi.deadlinemate.domain.gathering.event.GatheringCompletedEvent;
import com.fesi.deadlinemate.domain.gathering.event.GatheringCreatedEvent;
import com.fesi.deadlinemate.domain.gathering.event.GatheringDeletedEvent;
import com.fesi.deadlinemate.domain.gathering.event.GatheringUpdatedEvent;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringImageRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringTagRepository;
import com.fesi.deadlinemate.domain.gathering.repository.WeeklyPlanDetailRepository;
import com.fesi.deadlinemate.domain.gathering.repository.WeeklyPlanRepository;
import com.fesi.deadlinemate.domain.like.repository.GatheringLikeRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.global.common.ImageStorageService;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class GatheringServiceTest {

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private GatheringTagRepository gatheringTagRepository;

    @Mock
    private WeeklyPlanRepository weeklyPlanRepository;

    @Mock
    private WeeklyPlanDetailRepository weeklyPlanDetailRepository;

    @Mock
    private GatheringMemberRepository gatheringMemberRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private GatheringCategoryRepository gatheringCategoryRepository;

    @Mock
    private GatheringImageRepository gatheringImageRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private GatheringLikeRepository gatheringLikeRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GatheringService gatheringService;

    private CreateGatheringCommand createGatheringCommand;
    private UpdateGatheringCommand recruitingUpdateCommand;
    private UpdateGatheringCommand inProgressAllowedUpdateCommand;

    @BeforeEach
    void setUp() {
        createGatheringCommand = CreateGatheringCommand.builder()
                .leaderId(1L)
                .type(GatheringType.STUDY)
                .categoryIds(List.of(1L))
                .title("React 완전 정복 스터디")
                .shortDescription("리액트 공식문서를 같이 읽어요")
                .description("매주 공식문서 1챕터씩 읽고 블로그를 작성합니다...")
                .tags(List.of("React", "프론트엔드"))
                .goal("React 공식문서 완독 + 블로그 5편 작성")
                .maxMembers(6)
                .recruitDeadline(LocalDate.of(2025, 3, 20))
                .startDate(LocalDate.of(2025, 3, 22))
                .endDate(LocalDate.of(2025, 4, 19))
                .weeklyGuides(List.of(
                        new CreateGatheringCommand.CreateWeeklyGuideCommand(
                                1, "JSX, 컴포넌트, Props", List.of("공식문서 1~3챕터 읽기")
                        ),
                        new CreateGatheringCommand.CreateWeeklyGuideCommand(
                                2, "State, 이벤트 처리", List.of("공식문서 4~6챕터 읽기")
                        )
                ))
                .imageUrls(List.of())
                .build();

        recruitingUpdateCommand = UpdateGatheringCommand.builder()
                .requesterId(1L)
                .type(GatheringType.PROJECT)
                .categoryIds(List.of(1L))
                .title("Spring Boot 실전 프로젝트")
                .shortDescription("실전 백엔드 프로젝트 같이 해요")
                .description("요구사항 분석부터 배포까지 같이 진행합니다.")
                .goal("Spring Boot 기반 협업 프로젝트 완성")
                .tags(List.of("Spring", "백엔드"))
                .maxMembers(5)
                .recruitDeadline(LocalDate.of(2025, 3, 25))
                .startDate(LocalDate.of(2025, 3, 27))
                .endDate(LocalDate.of(2025, 5, 1))
                .weeklyGuides(List.of(
                        new UpdateGatheringCommand.UpdateWeeklyGuideCommand(
                                1, "1주차", List.of("기획 및 요구사항 정리")
                        ),
                        new UpdateGatheringCommand.UpdateWeeklyGuideCommand(
                                2, "2주차", List.of("도메인 설계")
                        )
                ))
                .imageUrls(null)
                .build();

        inProgressAllowedUpdateCommand = UpdateGatheringCommand.builder()
                .requesterId(1L)
                .type(GatheringType.STUDY)
                .categoryIds(List.of(1L))
                .title("React 완전 정복 스터디")
                .shortDescription("리액트 공식문서를 같이 읽어요")
                .description("진행 중 설명만 수정합니다.")
                .goal("React 공식문서 완독 + 블로그 5편 작성")
                .maxMembers(6)
                .recruitDeadline(LocalDate.of(2025, 3, 20))
                .startDate(LocalDate.of(2025, 3, 22))
                .endDate(LocalDate.of(2025, 4, 26))
                .tags(List.of("React", "프론트엔드"))
                .weeklyGuides(List.of(
                        new UpdateGatheringCommand.UpdateWeeklyGuideCommand(
                                1, "1주차 수정", List.of("진행 중 계획 수정")
                        ),
                        new UpdateGatheringCommand.UpdateWeeklyGuideCommand(
                                2, "2주차 수정", List.of("State 심화")
                        )
                ))
                .imageUrls(null)
                .build();
    }
    @Nested
    @DisplayName("모임 생성 테스트")
    class Create {
        @Test
        @DisplayName("모임을 생성할 수 있다")
        void createGathering() {
            // given
            given(userClient.existsById(1L)).willReturn(true);
            mockValidCategories();
            given(gatheringRepository.save(any(Gathering.class)))
                    .willAnswer(invocation -> {
                        Gathering gathering = invocation.getArgument(0);
                        setField(gathering, "id", 100L);
                        return gathering;
                    });
            given(gatheringMemberRepository.save(any(GatheringMember.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            given(weeklyPlanRepository.saveAll(anyList()))
                    .willAnswer(invocation -> {
                        List<WeeklyPlan> plans = invocation.getArgument(0);
                        for (int i = 0; i < plans.size(); i++) {
                            setField(plans.get(i), "id", (long) (i + 1));
                        }
                        return plans;
                    });

            given(weeklyPlanDetailRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            CreateGatheringResponse response = gatheringService.create(createGatheringCommand);

            // then
            assertThat(response.id()).isEqualTo(100L);

            ArgumentCaptor<Gathering> gatheringCaptor = ArgumentCaptor.forClass(Gathering.class);
            then(gatheringRepository).should().save(gatheringCaptor.capture());

            Gathering savedGathering = gatheringCaptor.getValue();
            assertThat(savedGathering.getLeaderId()).isEqualTo(1L);
            assertThat(savedGathering.getType()).isEqualTo(GatheringType.STUDY);
            assertThat(savedGathering.getTitle()).isEqualTo("React 완전 정복 스터디");
            assertThat(savedGathering.getShortDescription()).isEqualTo("리액트 공식문서를 같이 읽어요");
            assertThat(savedGathering.getDescription()).isEqualTo("매주 공식문서 1챕터씩 읽고 블로그를 작성합니다...");
            assertThat(savedGathering.getGoal()).isEqualTo("React 공식문서 완독 + 블로그 5편 작성");
            assertThat(savedGathering.getMaxMembers()).isEqualTo(6);
            assertThat(savedGathering.getCurrentMembers()).isEqualTo(1);
            assertThat(savedGathering.getRecruitDeadline()).isEqualTo(LocalDate.of(2025, 3, 20));
            assertThat(savedGathering.getStartDate()).isEqualTo(LocalDate.of(2025, 3, 22));
            assertThat(savedGathering.getEndDate()).isEqualTo(LocalDate.of(2025, 4, 19));
            assertThat(savedGathering.getTotalWeeks()).isEqualTo(5);
            assertThat(savedGathering.getStatus()).isEqualTo(GatheringStatus.RECRUITING);

            ArgumentCaptor<List<GatheringTag>> tagCaptor = ArgumentCaptor.forClass(List.class);
            then(gatheringTagRepository).should().saveAll(tagCaptor.capture());

            assertThat(tagCaptor.getValue())
                    .extracting(GatheringTag::getGatheringId, GatheringTag::getTag)
                    .containsExactly(
                            tuple(100L, "React"),
                            tuple(100L, "프론트엔드")
                    );

            ArgumentCaptor<List<WeeklyPlan>> weeklyPlanCaptor = ArgumentCaptor.forClass(List.class);
            then(weeklyPlanRepository).should().saveAll(weeklyPlanCaptor.capture());

            assertThat(weeklyPlanCaptor.getValue())
                    .extracting(
                            WeeklyPlan::getGatheringId,
                            WeeklyPlan::getWeekNumber,
                            WeeklyPlan::getTitle,
                            WeeklyPlan::getStartDate,
                            WeeklyPlan::getEndDate
                    )
                    .containsExactly(
                            tuple(
                                    100L,
                                    1,
                                    "JSX, 컴포넌트, Props",
                                    LocalDate.of(2025, 3, 22),
                                    LocalDate.of(2025, 3, 28)
                            ),
                            tuple(
                                    100L,
                                    2,
                                    "State, 이벤트 처리",
                                    LocalDate.of(2025, 3, 29),
                                    LocalDate.of(2025, 4, 4)
                            )
                    );

            ArgumentCaptor<GatheringMember> memberCaptor = ArgumentCaptor.forClass(GatheringMember.class);
            then(gatheringMemberRepository).should().save(memberCaptor.capture());

            ArgumentCaptor<List<GatheringCategory>> categoryCaptor = ArgumentCaptor.forClass(List.class);
            then(gatheringCategoryRepository).should().saveAll(categoryCaptor.capture());

            assertThat(categoryCaptor.getValue())
                    .extracting(GatheringCategory::getGatheringId, GatheringCategory::getCategoryId)
                    .containsExactly(tuple(100L, 1L));

            GatheringMember leaderMember = memberCaptor.getValue();
            assertThat(leaderMember.getGatheringId()).isEqualTo(100L);
            assertThat(leaderMember.getUserId()).isEqualTo(1L);
            assertThat(leaderMember.getRole()).isEqualTo(GatheringRole.LEADER);
            assertThat(leaderMember.isActive()).isTrue();

            ArgumentCaptor<List<WeeklyPlanDetail>> detailCaptor = ArgumentCaptor.forClass(List.class);
            then(weeklyPlanDetailRepository).should().saveAll(detailCaptor.capture());
            assertThat(detailCaptor.getValue())
                    .extracting(
                            WeeklyPlanDetail::getWeeklyPlanId,
                            WeeklyPlanDetail::getDisplayOrder,
                            WeeklyPlanDetail::getContent
                    )
                    .containsExactly(
                            tuple(1L, 0, "공식문서 1~3챕터 읽기"),
                            tuple(2L, 0, "공식문서 4~6챕터 읽기")
                    );

            ArgumentCaptor<GatheringCreatedEvent> eventCaptor = ArgumentCaptor.forClass(GatheringCreatedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());

            GatheringCreatedEvent event = eventCaptor.getValue();
            assertThat(event.gatheringId()).isEqualTo(100L);
            assertThat(event.leaderId()).isEqualTo(1L);
            assertThat(event.title()).isEqualTo("React 완전 정복 스터디");
        }

        @Test
        @DisplayName("존재하지 않는 유저는 모임을 생성할 수 없다")
        void failWhenLeaderDoesNotExist() {
            given(userClient.existsById(1L)).willReturn(false);

            assertThatThrownBy(() -> gatheringService.create(createGatheringCommand))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            then(gatheringRepository).should(never()).save(any());
            then(gatheringCategoryRepository).should(never()).saveAll(anyList());
            then(gatheringTagRepository).should(never()).saveAll(any());
            then(weeklyPlanRepository).should(never()).saveAll(any());
            then(gatheringMemberRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("최대 인원이 2명 미만이면 예외가 발생한다")
        void failWhenMaxMembersTooSmall() {
            CreateGatheringCommand command = createGatheringCommand.toBuilder()
                    .maxMembers(1)
                    .build();

            given(userClient.existsById(1L)).willReturn(true);
            mockValidCategories();

            assertThatThrownBy(() -> gatheringService.create(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_MAX_MEMBERS);

            then(gatheringRepository).should(never()).save(any());
            then(gatheringCategoryRepository).should(never()).saveAll(anyList());
            then(gatheringTagRepository).should(never()).saveAll(any());
            then(weeklyPlanRepository).should(never()).saveAll(any());
            then(gatheringMemberRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("모집 마감일이 시작일보다 늦으면 예외가 발생한다")
        void failWhenRecruitDeadlineAfterStartDate() {
            CreateGatheringCommand command = createGatheringCommand.toBuilder()
                    .recruitDeadline(LocalDate.of(2025, 3, 23))
                    .build();

            given(userClient.existsById(1L)).willReturn(true);
            mockValidCategories();

            assertThatThrownBy(() -> gatheringService.create(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_RECRUIT_DEADLINE);

            then(gatheringRepository).should(never()).save(any());
            then(gatheringCategoryRepository).should(never()).saveAll(anyList());
            then(gatheringTagRepository).should(never()).saveAll(any());
            then(weeklyPlanRepository).should(never()).saveAll(any());
            then(gatheringMemberRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("종료일이 시작일보다 빠르면 예외가 발생한다")
        void failWhenEndDateBeforeStartDate() {
            CreateGatheringCommand command = createGatheringCommand.toBuilder()
                    .endDate(LocalDate.of(2025, 3, 21))
                    .build();

            given(userClient.existsById(1L)).willReturn(true);
            mockValidCategories();

            assertThatThrownBy(() -> gatheringService.create(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_GATHERING_DATE);

            then(gatheringRepository).should(never()).save(any());
            then(gatheringTagRepository).should(never()).saveAll(any());
            then(weeklyPlanRepository).should(never()).saveAll(any());
            then(gatheringCategoryRepository).should(never()).saveAll(anyList());
            then(gatheringMemberRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("주차 가이드가 1주차부터 순차적이지 않으면 예외가 발생한다")
        void failWhenWeeklyGuideIsNotSequential() {
            CreateGatheringCommand command = createGatheringCommand.toBuilder()
                    .weeklyGuides(List.of(
                            new CreateGatheringCommand.CreateWeeklyGuideCommand(1, "1주차",List.of("내용")),
                            new CreateGatheringCommand.CreateWeeklyGuideCommand(3, "3주차", List.of("내용"))
                    ))
                    .build();

            given(userClient.existsById(1L)).willReturn(true);

            assertThatThrownBy(() -> gatheringService.create(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_WEEKLY_GUIDE_SEQUENCE);

            then(gatheringRepository).should(never()).save(any());
            then(gatheringCategoryRepository).should(never()).saveAll(anyList());
            then(gatheringTagRepository).should(never()).saveAll(anyList());
            then(weeklyPlanRepository).should(never()).saveAll(anyList());
            then(gatheringMemberRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("모임 수정 테스트")
    class Update {

        @Test
        @DisplayName("진행 중 상태에서는 description, weeklyGuides, endDate만 수정할 수 있다")
        void updateInProgressGatheringAllowedFieldsOnly() {
            // given
            Gathering gathering = inProgressGathering();
            given(gatheringRepository.findById(100L)).willReturn(Optional.of(gathering));

            given(gatheringTagRepository.findTagsByGatheringId(100L))
                    .willReturn(List.of("React", "프론트엔드"));

            WeeklyPlan existingPlan1 = WeeklyPlan.builder()
                    .gatheringId(100L)
                    .weekNumber(1)
                    .title("기존 1주차")
                    .startDate(LocalDate.of(2025, 3, 22))
                    .endDate(LocalDate.of(2025, 3, 28))
                    .build();
            setField(existingPlan1, "id", 11L);

            WeeklyPlan existingPlan2 = WeeklyPlan.builder()
                    .gatheringId(100L)
                    .weekNumber(2)
                    .title("기존 2주차")
                    .startDate(LocalDate.of(2025, 3, 29))
                    .endDate(LocalDate.of(2025, 4, 4))
                    .build();
            setField(existingPlan2, "id", 12L);

            given(weeklyPlanRepository.findByGatheringIdOrderByWeekNumberAsc(100L))
                    .willReturn(List.of(existingPlan1, existingPlan2));

            given(weeklyPlanRepository.saveAll(anyList()))
                    .willAnswer(invocation -> {
                        List<WeeklyPlan> plans = invocation.getArgument(0);
                        for (int i = 0; i < plans.size(); i++) {
                            setField(plans.get(i), "id", (long) (101 + i));
                        }
                        return plans;
                    });

            given(weeklyPlanDetailRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            mockCurrentGatheringCategory(100L, 1L);

            // when
            UpdateGatheringResponse response = gatheringService.update(100L, inProgressAllowedUpdateCommand);

            // then
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.tags()).containsExactly("React", "프론트엔드");
            assertThat(gathering.getType()).isEqualTo(GatheringType.STUDY);
            assertThat(gathering.getTitle()).isEqualTo("React 완전 정복 스터디");
            assertThat(gathering.getShortDescription()).isEqualTo("리액트 공식문서를 같이 읽어요");
            assertThat(gathering.getGoal()).isEqualTo("React 공식문서 완독 + 블로그 5편 작성");
            assertThat(gathering.getMaxMembers()).isEqualTo(6);
            assertThat(gathering.getRecruitDeadline()).isEqualTo(LocalDate.of(2025, 3, 20));
            assertThat(gathering.getStartDate()).isEqualTo(LocalDate.of(2025, 3, 22));
            assertThat(gathering.getDescription()).isEqualTo("진행 중 설명만 수정합니다.");
            assertThat(gathering.getEndDate()).isEqualTo(LocalDate.of(2025, 4, 26));
            assertThat(gathering.getTotalWeeks()).isEqualTo(6);

            then(gatheringTagRepository).should(never()).deleteByGatheringId(anyLong());
            then(gatheringTagRepository).should(never()).saveAll(anyList());
            then(gatheringTagRepository).should().findTagsByGatheringId(100L);

            then(weeklyPlanDetailRepository).should().deleteByWeeklyPlanIdIn(List.of(11L, 12L));
            then(weeklyPlanRepository).should().deleteByGatheringId(100L);
            then(weeklyPlanRepository).should().saveAll(anyList());
            then(weeklyPlanDetailRepository).should().saveAll(anyList());

            then(gatheringImageRepository).shouldHaveNoInteractions();
            then(imageStorageService).shouldHaveNoInteractions();

            ArgumentCaptor<GatheringUpdatedEvent> eventCaptor =
                    ArgumentCaptor.forClass(GatheringUpdatedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());

            GatheringUpdatedEvent event = eventCaptor.getValue();
            assertThat(event.gatheringId()).isEqualTo(100L);
            assertThat(event.leaderId()).isEqualTo(1L);
            assertThat(event.status()).isEqualTo(GatheringStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("진행 중 상태에서 수정 불가능한 필드를 바꾸면 예외가 발생한다")
        void failWhenModifyForbiddenFieldInProgress() {
            // given
            Gathering gathering = inProgressGathering();
            given(gatheringRepository.findById(100L)).willReturn(Optional.of(gathering));
            given(gatheringTagRepository.findTagsByGatheringId(100L))
                    .willReturn(List.of("React", "프론트엔드"));
            mockCurrentGatheringCategory(100L, 1L);

            UpdateGatheringCommand command = inProgressAllowedUpdateCommand.toBuilder()
                    .title("진행 중인데 제목 바꿈")
                    .build();

            // when & then
            assertThatThrownBy(() -> gatheringService.update(100L, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_IN_PROGRESS_UPDATE_ITEMS);

            then(gatheringRepository).should().findById(100L);
            then(gatheringTagRepository).should().findTagsByGatheringId(100L);
            then(gatheringTagRepository).should(never()).deleteByGatheringId(anyLong());
            then(gatheringTagRepository).should(never()).saveAll(anyList());
            then(weeklyPlanRepository).should(never()).deleteByGatheringId(anyLong());
            then(weeklyPlanRepository).should(never()).saveAll(anyList());
            then(gatheringCategoryRepository).should(never()).deleteByGatheringId(anyLong());
            then(gatheringCategoryRepository).should(never()).saveAll(anyList());
            then(gatheringMemberRepository).shouldHaveNoInteractions();
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("모임장이 아니면 수정할 수 없다")
        void failWhenRequesterIsNotLeader() {
            // given
            Gathering gathering = recruitingGathering();
            given(gatheringRepository.findById(100L)).willReturn(Optional.of(gathering));

            UpdateGatheringCommand command = recruitingUpdateCommand.toBuilder()
                    .requesterId(999L)
                    .build();

            // when & then
            assertThatThrownBy(() -> gatheringService.update(100L, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_GATHERING_LEADER);

            then(gatheringTagRepository).should(never()).deleteByGatheringId(anyLong());
            then(gatheringTagRepository).should(never()).saveAll(anyList());
            then(gatheringCategoryRepository).should(never()).deleteByGatheringId(anyLong());
            then(gatheringCategoryRepository).should(never()).saveAll(anyList());
            then(gatheringTagRepository).should(never()).findTagsByGatheringId(anyLong());
            then(weeklyPlanRepository).should(never()).deleteByGatheringId(anyLong());
            then(weeklyPlanRepository).should(never()).saveAll(anyList());
            then(eventPublisher).should(never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("모임 삭제 테스트")
    class Delete {

        @Test
        @DisplayName("모임 삭제 시 관련 데이터가 모두 삭제되고 이벤트가 발행된다")
        void deleteSuccess() {
            Gathering gathering = recruitingGathering();
            given(gatheringRepository.findById(100L)).willReturn(Optional.of(gathering));

            WeeklyPlan plan = WeeklyPlan.builder()
                    .gatheringId(100L).weekNumber(1).title("1주차")
                    .startDate(LocalDate.of(2025, 3, 22)).endDate(LocalDate.of(2025, 3, 28))
                    .build();
            setField(plan, "id", 11L);
            given(weeklyPlanRepository.findByGatheringIdOrderByWeekNumberAsc(100L))
                    .willReturn(List.of(plan));

            gatheringService.delete(100L, 1L);

            then(weeklyPlanDetailRepository).should().deleteByWeeklyPlanIdIn(List.of(11L));
            then(weeklyPlanRepository).should().deleteByGatheringId(100L);
            then(gatheringCategoryRepository).should().deleteByGatheringId(100L);
            then(gatheringTagRepository).should().deleteByGatheringId(100L);
            then(gatheringMemberRepository).should().deleteByGatheringId(100L);
            then(gatheringLikeRepository).should().deleteByGatheringId(100L);
            then(gatheringRepository).should().delete(gathering);

            ArgumentCaptor<GatheringDeletedEvent> eventCaptor = ArgumentCaptor.forClass(GatheringDeletedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().gatheringId()).isEqualTo(100L);
            assertThat(eventCaptor.getValue().leaderId()).isEqualTo(1L);
            assertThat(eventCaptor.getValue().title()).isEqualTo("React 완전 정복 스터디");
        }

        @Test
        @DisplayName("모임장이 아닌 유저가 삭제 시 예외가 발생한다")
        void failWhenRequesterIsNotLeader() {
            Gathering gathering = recruitingGathering();
            given(gatheringRepository.findById(100L)).willReturn(Optional.of(gathering));

            assertThatThrownBy(() -> gatheringService.delete(100L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_GATHERING_LEADER);

            then(gatheringRepository).should(never()).delete(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("진행 중인 모임은 삭제할 수 없다")
        void failWhenGatheringIsInProgress() {
            Gathering gathering = inProgressGathering();
            given(gatheringRepository.findById(100L)).willReturn(Optional.of(gathering));

            assertThatThrownBy(() -> gatheringService.delete(100L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.GATHERING_DELETE_NOT_ALLOWED);

            then(gatheringRepository).should(never()).delete(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("모임 완료 처리 테스트")
    class CompleteEndedGatherings {

        @Test
        @DisplayName("종료일이 지난 IN_PROGRESS 모임들을 완료 처리하고 이벤트를 발행한다")
        void completesEndedGatherings() {
            Gathering g1 = inProgressGathering(); // id=100, leaderId=1

            Gathering g2 = Gathering.builder()
                    .leaderId(2L)
                    .type(GatheringType.STUDY)
                    .title("다른 스터디")
                    .shortDescription("짧은 소개")
                    .description("설명")
                    .goal("목표")
                    .maxMembers(4)
                    .currentMembers(4)
                    .recruitDeadline(LocalDate.of(2025, 2, 1))
                    .startDate(LocalDate.of(2025, 2, 3))
                    .endDate(LocalDate.of(2025, 3, 1))
                    .totalWeeks(4)
                    .status(GatheringStatus.IN_PROGRESS)
                    .viewCount(5)
                    .build();
            setField(g2, "id", 200L);

            LocalDate today = LocalDate.of(2025, 4, 19);
            given(gatheringRepository.findByStatusAndEndDateLessThanEqual(GatheringStatus.IN_PROGRESS, today))
                    .willReturn(List.of(g1, g2));

            gatheringService.completeEndedGatherings(today);

            assertThat(g1.getStatus()).isEqualTo(GatheringStatus.COMPLETED);
            assertThat(g2.getStatus()).isEqualTo(GatheringStatus.COMPLETED);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            then(eventPublisher).should(times(2)).publishEvent(eventCaptor.capture());

            List<Object> events = eventCaptor.getAllValues();
            assertThat(events).allMatch(e -> e instanceof GatheringCompletedEvent);

            GatheringCompletedEvent event1 = (GatheringCompletedEvent) events.get(0);
            assertThat(event1.gatheringId()).isEqualTo(100L);
            assertThat(event1.leaderId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("완료 처리할 모임이 없으면 아무것도 하지 않는다")
        void doesNothingWhenNoGatheringsToComplete() {
            LocalDate today = LocalDate.of(2025, 4, 19);
            given(gatheringRepository.findByStatusAndEndDateLessThanEqual(GatheringStatus.IN_PROGRESS, today))
                    .willReturn(List.of());

            gatheringService.completeEndedGatherings(today);

            then(eventPublisher).should(never()).publishEvent(any());
        }
    }

    private Gathering recruitingGathering() {
        Gathering gathering = Gathering.builder()
                .leaderId(1L)
                .type(GatheringType.STUDY)
                .title("React 완전 정복 스터디")
                .shortDescription("리액트 공식문서를 같이 읽어요")
                .description("매주 공식문서 1챕터씩 읽고 블로그를 작성합니다...")
                .goal("React 공식문서 완독 + 블로그 5편 작성")
                .maxMembers(6)
                .currentMembers(1)
                .recruitDeadline(LocalDate.of(2025, 3, 20))
                .startDate(LocalDate.of(2025, 3, 22))
                .endDate(LocalDate.of(2025, 4, 19))
                .totalWeeks(5)
                .status(GatheringStatus.RECRUITING)
                .viewCount(0)
                .build();
        setField(gathering, "id", 100L);
        return gathering;
    }

    private Gathering inProgressGathering() {
        Gathering gathering = Gathering.builder()
                .leaderId(1L)
                .type(GatheringType.STUDY)
                .title("React 완전 정복 스터디")
                .shortDescription("리액트 공식문서를 같이 읽어요")
                .description("기존 설명")
                .goal("React 공식문서 완독 + 블로그 5편 작성")
                .maxMembers(6)
                .currentMembers(3)
                .recruitDeadline(LocalDate.of(2025, 3, 20))
                .startDate(LocalDate.of(2025, 3, 22))
                .endDate(LocalDate.of(2025, 4, 19))
                .totalWeeks(5)
                .status(GatheringStatus.IN_PROGRESS)
                .viewCount(10)
                .build();
        setField(gathering, "id", 100L);
        return gathering;
    }

    private void mockValidCategories() {
        Category category = Category.builder().name("개발").build();
        setField(category, "id", 1L);

        given(categoryRepository.findByIdIn(anyList()))
                .willReturn(List.of(category));
    }

    private void mockCurrentGatheringCategory(Long gatheringId, Long categoryId) {
        given(gatheringCategoryRepository.findByGatheringId(gatheringId))
                .willReturn(List.of(
                        GatheringCategory.builder()
                                .gatheringId(gatheringId)
                                .categoryId(categoryId)
                                .build()
                ));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}