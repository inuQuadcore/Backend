package com.everybuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "로그인 응답")
public class LoginResponse {

    @Schema(description = "사용자 ID", example = "123")
    private final Long userId;

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String accessToken;

    @Schema(description = "JWT 리프레쉬 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String refreshToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private final String tokenType = "Bearer";

    @Schema(description = "액세스 토큰 만료 시각", example = "2026-04-30T13:00:00")
    private final LocalDateTime accessTokenExpiresAt;

    @Schema(description = "리프레쉬 토큰 만료 시각", example = "2026-04-30T13:00:00")
    private final LocalDateTime refreshTokenExpiresAt;

    @Builder
    private LoginResponse(Long userId, String accessToken, String refreshToken,
                          LocalDateTime accessTokenExpiresAt, LocalDateTime refreshTokenExpiresAt) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public static LoginResponse of(Long userId, String accessToken, LocalDateTime accessTokenExpiresAt,
                                   String refreshToken, LocalDateTime refreshTokenExpiresAt) {
        return LoginResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .build();
    }
}
