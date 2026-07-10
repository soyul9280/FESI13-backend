package com.fesi.deadlinemate.domain.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.fesi.deadlinemate.domain.notification.command.SendNotificationCommand;
import com.fesi.deadlinemate.domain.notification.entity.NotificationType;
import com.fesi.deadlinemate.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private static final Map<NotificationType, String> TITLES = Map.of(
            NotificationType.APPLICATION_RECEIVED, "새 가입 신청",
            NotificationType.APPLICATION_ACCEPTED, "신청 수락",
            NotificationType.APPLICATION_REJECTED, "신청 거절",
            NotificationType.PENALTY_WARNING,      "평판 점수 조정",
            NotificationType.GATHERING_STARTED,    "모임 시작",
            NotificationType.GATHERING_ENDED,      "모임 종료",
            NotificationType.REVIEW_REQUEST,       "리뷰 요청",
            NotificationType.POKE,                 "콕! 찔림"
    );

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public void sendToUser(SendNotificationCommand command) {
        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }

        List<String> tokens = fcmTokenRepository.findAllByUserId(command.userId())
                .stream()
                .map(t -> t.getToken())
                .toList();

        if (tokens.isEmpty()) {
            return;
        }

        String title = TITLES.getOrDefault(command.type(), "Deadline Mate");

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(command.content())
                        .build())
                .putData("type", command.type().name())
                .setWebpushConfig(WebpushConfig.builder()
                        .setFcmOptions(WebpushFcmOptions.builder()
                                .setLink(command.targetUrl())
                                .build())
                        .build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            removeDeadTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패: {}", e.getMessage());
        }
    }

    private void removeDeadTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sr = responses.get(i);
            if (!sr.isSuccessful()) {
                MessagingErrorCode errorCode = sr.getException().getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    fcmTokenRepository.deleteByToken(tokens.get(i));
                    log.debug("만료된 FCM 토큰 삭제: {}", tokens.get(i));
                }
            }
        }
    }
}