package com.fesi.deadlinemate.domain.todo.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fesi.deadlinemate.domain.todo.command.CreateTodoCommand;
import com.fesi.deadlinemate.domain.todo.command.UpdateTodoCommand;
import com.fesi.deadlinemate.domain.todo.dto.request.CreateTodoRequest;
import com.fesi.deadlinemate.domain.todo.dto.request.UpdateTodoRequest;
import com.fesi.deadlinemate.domain.todo.dto.response.CreateTodoResponse;
import com.fesi.deadlinemate.domain.todo.dto.response.MyTodoListResponse;
import com.fesi.deadlinemate.domain.todo.dto.response.UpdateTodoResponse;
import com.fesi.deadlinemate.domain.todo.service.TodoService;
import com.fesi.deadlinemate.global.security.JwtTokenProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TodoService todoService;

    private String token;
    private CreateTodoRequest createRequest;
    private UpdateTodoRequest updateRequest;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateAccessToken(1L, "user1@test.com");
        createRequest = createTodoRequest();
        updateRequest = updateTodoRequest();
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/{gatheringId}/todos")
    class CreateTodo {

        @Test
        @DisplayName("인증된 사용자는 Todo를 생성할 수 있다")
        void createTodo_success() throws Exception {
            given(todoService.create(any(CreateTodoCommand.class)))
                    .willReturn(createTodoResponse());

            mockMvc.perform(post("/api/v1/gatherings/{gatheringId}/todos", 1L)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest))
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100))
                    .andExpect(jsonPath("$.data.week").value(3))
                    .andExpect(jsonPath("$.data.content").value("공식문서 7챕터 읽기"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/gatherings/{gatheringId}/todos/{todoId}")
    class UpdateTodo {

        @Test
        @DisplayName("인증된 사용자는 Todo를 수정할 수 있다")
        void updateTodo_success() throws Exception {
            given(todoService.update(any(UpdateTodoCommand.class)))
                    .willReturn(updateTodoResponse());

            mockMvc.perform(patch("/api/v1/gatherings/{gatheringId}/todos/{todoId}", 1L, 100L)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100))
                    .andExpect(jsonPath("$.data.content").value("수정된 할 일"))
                    .andExpect(jsonPath("$.data.isCompleted").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/{gatheringId}/todos/me")
    class GetMyTodos {

        @Test
        @DisplayName("인증된 사용자는 내 Todo 목록과 달성률을 조회할 수 있다")
        void getMyTodos_success() throws Exception {
            given(todoService.getMyTodos(1L, 1L, 3))
                    .willReturn(myTodoListResponse());

            mockMvc.perform(get("/api/v1/gatherings/{gatheringId}/todos/me", 1L)
                            .header("Authorization", "Bearer " + token)
                            .param("week", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.todos[0].id").value(100))
                    .andExpect(jsonPath("$.data.todos[0].content").value("공식문서 7챕터 읽기"))
                    .andExpect(jsonPath("$.data.weeklyAchievementRate").value(50.0))
                    .andExpect(jsonPath("$.data.overallAchievementRate").value(75.0))
                    .andExpect(jsonPath("$.data.streakDays").value(5));
        }

        @Test
        @DisplayName("인증 없이 내 Todo 조회 시 403을 반환한다")
        void getMyTodos_withoutAuth_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/gatherings/{gatheringId}/todos/me", 1L))
                    .andExpect(status().isForbidden());
        }
    }

    private CreateTodoRequest createTodoRequest() {
        return CreateTodoRequest.builder()
                .week(3)
                .content("공식문서 7챕터 읽기")
                .build();
    }

    private UpdateTodoRequest updateTodoRequest() {
        return UpdateTodoRequest.builder()
                .content("수정된 할 일")
                .isCompleted(true)
                .build();
    }

    private CreateTodoResponse createTodoResponse() {
        return CreateTodoResponse.builder()
                .id(100L)
                .week(3)
                .content("공식문서 7챕터 읽기")
                .isCompleted(false)
                .createdAt(LocalDateTime.of(2026, 4, 10, 12, 0))
                .build();
    }

    private UpdateTodoResponse updateTodoResponse() {
        return UpdateTodoResponse.builder()
                .id(100L)
                .content("수정된 할 일")
                .isCompleted(true)
                .build();
    }

    private MyTodoListResponse myTodoListResponse() {
        return MyTodoListResponse.of(
                List.of(
                        MyTodoListResponse.MyTodoItemResponse.builder()
                                .id(100L)
                                .week(3)
                                .content("공식문서 7챕터 읽기")
                                .isCompleted(false)
                                .createdAt(LocalDateTime.of(2026, 4, 10, 12, 0))
                                .build()
                ),
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(75.0),
                5
        );
    }
}