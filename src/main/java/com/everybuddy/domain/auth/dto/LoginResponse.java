package com.everybuddy.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginResponse {
    private final Long userId;

    private final String accessToken;

    private final String tokenType = "Bearer";

    private final Long expireIn;

    @Builder
    private LoginResponse(Long userId, String accessToken, String tokenType, Long expireIn) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.expireIn = expireIn;
    }

    public static LoginResponse of(Long userId, String accessToken, Long expireInMillis) {
        return LoginResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .expireIn(expireInMillis / 1000)  // 밀리초 → 초 변환
                .build();
    }
}
