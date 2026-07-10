package com.fesi.deadlinemate.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fesi.deadlinemate.domain.notification.command.SendNotificationCommand;
import com.fesi.deadlinemate.domain.notification.entity.Notification;
import com.fesi.deadlinemate.domain.notification.entity.NotificationType;
import com.fesi.deadlinemate.domain.notification.entity.ReferenceType;
import com.fesi.deadlinemate.domain.notification.repository.NotificationRepository;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FcmPushService fcmPushService;

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Nested
    @DisplayName("알림 발송")
    class Send {

        @Test
        @DisplayName("Command를 받아 알림을 저장한다")
        void send() {
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            SendNotificationCommand command = new SendNotificationCommand(
                    1L, NotificationType.APPLICATION_ACCEPTED,
                    "'React 스터디' 참여가 수락되었어요!", "/gatherings/1/dashboard",
                    1L, ReferenceType.GATHERING
            );

            notificationCommandService.send(command);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            then(notificationRepository).should().save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
            assertThat(captor.getValue().isRead()).isFalse();
        }
    }

    @Nested
    @DisplayName("단일 알림 읽음 처리")
    class MarkAsRead {

        @Test
        @DisplayName("알림을 읽음으로 표시한다")
        void markAsRead() {
            Notification notification = notification(10L, 1L);
            given(notificationRepository.findById(10L)).willReturn(Optional.of(notification));

            notificationCommandService.markAsRead(10L, 1L);
            assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 알림이면 예외가 발생한다")
        void notFoundThrows() {
            given(notificationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationCommandService.markAsRead(99L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("본인 알림이 아니면 엔티티에서 예외가 발생한다")
        void forbiddenThrows() {
            Notification notification = notification(10L, 1L);
            given(notificationRepository.findById(10L)).willReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationCommandService.markAsRead(10L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("전체 알림 읽음 처리")
    class MarkAllAsRead {

        @Test
        @DisplayName("repository의 markAllAsRead를 호출한다")
        void markAllAsRead() {
            notificationCommandService.markAllAsRead(1L);
            then(notificationRepository).should().markAllAsRead(1L);
        }
    }

    private Notification notification(Long id, Long userId) {
        Notification n = Notification.builder()
                .userId(userId).type(NotificationType.APPLICATION_ACCEPTED)
                .content("테스트").targetUrl("/test").build();
        try {
            Field f = Notification.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(n, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        return n;
    }
}
