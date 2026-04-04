package com.everybuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "Google OAuth 로그인 응답")
public class GoogleLoginResponse {

    @Schema(description = "신규 유저 여부", example = "true")
    private final boolean isNewUser;

    @Schema(description = "임시 토큰 (신규 유저일 때만 반환)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String tempToken;

    @Schema(description = "로그인 정보 (기존 유저일 때만 반환)")
    private final LoginResponse loginData;

    @Builder
    private GoogleLoginResponse(boolean isNewUser, String tempToken, LoginResponse loginData) {
        this.isNewUser = isNewUser;
        this.tempToken = tempToken;
        this.loginData = loginData;
    }

    public static GoogleLoginResponse newUser(String tempToken) {
        return GoogleLoginResponse.builder()
                .isNewUser(true)
                .tempToken(tempToken)
                .build();
    }

    public static GoogleLoginResponse existingUser(LoginResponse loginData) {
        return GoogleLoginResponse.builder()
                .isNewUser(false)
                .loginData(loginData)
                .build();
    }
}
