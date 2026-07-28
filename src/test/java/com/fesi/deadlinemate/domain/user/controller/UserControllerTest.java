package com.fesi.deadlinemate.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fesi.deadlinemate.domain.user.dto.request.ChangePasswordRequest;
import com.fesi.deadlinemate.domain.user.dto.request.UpdateProfileRequest;
import com.fesi.deadlinemate.domain.user.entity.Provider;
import com.fesi.deadlinemate.domain.user.entity.User;
import com.fesi.deadlinemate.domain.user.service.UserService;
import com.fesi.deadlinemate.domain.user.service.UserStatsQueryService;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import com.fesi.deadlinemate.global.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private UserStatsQueryService userStatsQueryService;

    @Mock
    private com.fesi.deadlinemate.domain.gathering.client.GatheringClient gatheringClient;

    @Mock
    private com.fesi.deadlinemate.global.common.ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("내 프로필을 조회할 수 있다")
    void getMyProfile() throws Exception {
        User user = createTestUser(1L);
        given(userService.findById(1L)).willReturn(user);
        given(userStatsQueryService.countCompletedGatherings(1L)).willReturn(3L);
        given(userStatsQueryService.calculateAvgAchievementRate(1L)).willReturn(BigDecimal.valueOf(75.0));
        given(userStatsQueryService.countReviews(1L)).willReturn(5L);

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("마감왕"))
                .andExpect(jsonPath("$.data.provider").value("EMAIL"))
                .andExpect(jsonPath("$.data.completedGatherings").value(3))
                .andExpect(jsonPath("$.data.reviewCount").value(5));
    }

    @Test
    @DisplayName("프로필을 수정할 수 있다")
    void updateProfile() throws Exception {
        User updatedUser = createTestUser(1L);
        given(userService.updateProfile(eq(1L), anyString(), any())).willReturn(updatedUser);
        given(userStatsQueryService.countCompletedGatherings(1L)).willReturn(0L);
        given(userStatsQueryService.calculateAvgAchievementRate(1L)).willReturn(BigDecimal.ZERO);
        given(userStatsQueryService.countReviews(1L)).willReturn(0L);

        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", null);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/v1/users/me")
                        .file(requestPart)
                        .with(req -> { req.setMethod("PATCH"); return req; })
                        .principal(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("비밀번호를 변경할 수 있다")
    void changePassword() throws Exception {
        doNothing().when(userService).changePassword(eq(1L), anyString(), anyString());

        ChangePasswordRequest request = new ChangePasswordRequest("OldP@ss1!", "NewP@ss1!");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .principal(new UsernamePasswordAuthenticationToken(1L, null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("소셜 유저가 비밀번호 변경 시 403을 반환한다")
    void changePasswordForbiddenForSocialUser() throws Exception {
        doThrow(new BusinessException(ErrorCode.SOCIAL_USER_PASSWORD_CHANGE))
                .when(userService).changePassword(eq(1L), anyString(), anyString());

        ChangePasswordRequest request = new ChangePasswordRequest("OldP@ss1!", "NewP@ss1!");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .principal(new UsernamePasswordAuthenticationToken(1L, null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SOCIAL_USER_PASSWORD_CHANGE"));
    }

    @Test
    @DisplayName("다른 유저의 공개 프로필을 조회할 수 있다")
    void getPublicProfile() throws Exception {
        User user = createTestUser(2L);
        given(userService.findById(2L)).willReturn(user);

        mockMvc.perform(get("/api/v1/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("마감왕"))
                .andExpect(jsonPath("$.data.reputationLabel").value("연기 메이트"));
    }

    @Test
    @DisplayName("회원 탈퇴를 할 수 있다")
    void deleteAccount() throws Exception {
        doNothing().when(userService).deactivate(1L);

        mockMvc.perform(delete("/api/v1/users/me")
                        .principal(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private User createTestUser(Long id) {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hash")
                .nickname("마감왕")
                .provider(Provider.EMAIL)
                .build();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
