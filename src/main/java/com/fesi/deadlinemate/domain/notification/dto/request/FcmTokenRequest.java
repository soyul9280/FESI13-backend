package com.fesi.deadlinemate.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank String token
) {}
