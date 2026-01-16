package com.everybuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "로그인 응답")
public class LoginResponse {

    @Schema(description = "사용자 ID", example = "123")
    private final Long userId;

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private final String tokenType = "Bearer";

    @Schema(description = "토큰 만료 시간 (초)", example = "86400")
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
