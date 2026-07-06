package com.fesi.deadlinemate.domain.notification.controller;

import com.fesi.deadlinemate.domain.notification.dto.request.FcmTokenRequest;
import com.fesi.deadlinemate.domain.notification.service.FcmTokenService;
import com.fesi.deadlinemate.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push/tokens")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    public ApiResponse<Void> register(
            @Valid @RequestBody FcmTokenRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String userAgent = httpRequest.getHeader("User-Agent");
        fcmTokenService.register(userId, request.token(), userAgent);
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<Void> delete(
            @Valid @RequestBody FcmTokenRequest request,
            Authentication authentication
    ) {
        fcmTokenService.delete(request.token());
        return ApiResponse.success();
    }
}
