package com.everybuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "Firebase 커스텀 토큰 응답")
public class FirebaseTokenResponse {

    @Schema(description = "Firebase 커스텀 토큰", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String firebaseToken;

    @Builder
    private FirebaseTokenResponse(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }

    public static FirebaseTokenResponse of(String firebaseToken) {
        return FirebaseTokenResponse.builder()
                .firebaseToken(firebaseToken)
                .build();
    }
}
