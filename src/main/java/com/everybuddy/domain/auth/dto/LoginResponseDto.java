package com.everybuddy.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginResponseDto {
    private final String accessToken;

    private final String tokenType = "Bearer";

    private final Long expireIn;

    @Builder
    private LoginResponseDto(String accessToken, String tokenType, Long expireIn) {
        this.accessToken = accessToken;
        this.expireIn = expireIn;
    }

    public static LoginResponseDto of(String accessToken, Long expireInMillis) {
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .expireIn(expireInMillis / 1000)  // 밀리초 → 초 변환
                .build();
    }
}
