package com.everybuddy.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginResponse {
    private final String accessToken;

    private final String tokenType = "Bearer";

    private final Long expireIn;

    @Builder
    private LoginResponse(String accessToken, String tokenType, Long expireIn) {
        this.accessToken = accessToken;
        this.expireIn = expireIn;
    }

    public static LoginResponse of(String accessToken, Long expireInMillis) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .expireIn(expireInMillis / 1000)  // 밀리초 → 초 변환
                .build();
    }
}
