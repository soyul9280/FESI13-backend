package com.fesi.deadlinemate.domain.notification.service;

import com.fesi.deadlinemate.domain.notification.entity.FcmToken;
import com.fesi.deadlinemate.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    public void register(Long userId, String token, String userAgent) {
        if (fcmTokenRepository.existsByToken(token)) {
            return;
        }
        fcmTokenRepository.save(FcmToken.builder()
                .userId(userId)
                .token(token)
                .userAgent(userAgent)
                .build());
    }

    public void delete(String token) {
        fcmTokenRepository.deleteByToken(token);
    }
}
