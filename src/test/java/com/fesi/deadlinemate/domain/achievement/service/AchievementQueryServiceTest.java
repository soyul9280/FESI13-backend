package com.fesi.deadlinemate.domain.achievement.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fesi.deadlinemate.domain.achievement.dto.response.AchievementResponse;
import com.fesi.deadlinemate.domain.gathering.entity.Gathering;
import com.fesi.deadlinemate.domain.gathering.entity.GatheringMember;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringMemberRepository;
import com.fesi.deadlinemate.domain.gathering.repository.GatheringRepository;
import com.fesi.deadlinemate.domain.todo.entity.Todo;
import com.fesi.deadlinemate.domain.todo.repository.TodoRepository;
import com.fesi.deadlinemate.domain.user.client.UserClient;
import com.fesi.deadlinemate.domain.user.client.dto.UserInfo;
import com.fesi.deadlinemate.global.error.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AchievementQueryServiceTest {

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private GatheringMemberRepository gatheringMemberRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private AchievementQueryService achievementQueryService;

    private final Long GATHERING_ID = 1L;
    private final Long USER_ID_1 = 10L;
    private final Long USER_ID_2 = 20L;

    @Test
    @DisplayName("정상적으로 조회가 된다.")
    void getAchievements_success() {
        // given
        Gathering gathering = mock(Gathering.class);
        when(gathering.getTotalWeeks()).thenReturn(2);

        when(gatheringRepository.findById(GATHERING_ID))
                .thenReturn(Optional.of(gathering));

        when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(GATHERING_ID, USER_ID_1))
                .thenReturn(true);

        GatheringMember member1 = mock(GatheringMember.class);
        GatheringMember member2 = mock(GatheringMember.class);

        when(member1.getUserId()).thenReturn(USER_ID_1);
        when(member2.getUserId()).thenReturn(USER_ID_2);

        when(gatheringMemberRepository.findByGatheringIdAndIsActiveTrueOrderByIdAsc(GATHERING_ID))
                .thenReturn(List.of(member1, member2));

        Todo todo1 = mock(Todo.class);
        Todo todo2 = mock(Todo.class);

        when(todo1.getUserId()).thenReturn(USER_ID_1);
        when(todo1.getWeekNumber()).thenReturn(1);
        when(todo1.isCompleted()).thenReturn(true);

        when(todo2.getUserId()).thenReturn(USER_ID_2);
        when(todo2.getWeekNumber()).thenReturn(1);
        when(todo2.isCompleted()).thenReturn(false);

        when(todoRepository.findByGatheringIdOrderByWeekNumberAscCreatedAtAsc(GATHERING_ID))
                .thenReturn(List.of(todo1, todo2));

        UserInfo user1 = mock(UserInfo.class);
        when(user1.getNickname()).thenReturn("user1");

        UserInfo user2 = mock(UserInfo.class);
        when(user2.getNickname()).thenReturn("user2");

        when(userClient.findByIds(List.of(USER_ID_1, USER_ID_2)))
                .thenReturn(Map.of(USER_ID_1, user1, USER_ID_2, user2));

        // when
        AchievementResponse response =
                achievementQueryService.getAchievements(GATHERING_ID, USER_ID_1);

        // then
        assertThat(response.members()).hasSize(2);
        assertThat(response.teamOverallRate()).isEqualByComparingTo("50.0");
    }

    @Test
    @DisplayName("비회원이면 에러가 발생한다.")
    void getAchievements_fail() {
        // given
        when(gatheringRepository.findById(GATHERING_ID))
                .thenReturn(Optional.of(mock(Gathering.class)));

        when(gatheringMemberRepository.existsByGatheringIdAndUserIdAndIsActiveTrue(GATHERING_ID, USER_ID_1))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() ->
                achievementQueryService.getAchievements(GATHERING_ID, USER_ID_1))
                .isInstanceOf(BusinessException.class);
    }
}