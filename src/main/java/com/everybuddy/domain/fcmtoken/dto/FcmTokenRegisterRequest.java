package com.everybuddy.domain.fcmtoken.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "FCM 토큰 등록 요청")
public class FcmTokenRegisterRequest {

    @NotBlank
    @Schema(description = "FCM 디바이스 토큰", example = "fGx9KhOaSWmZ...")
    private String token;

    private FcmTokenRegisterRequest() {}

    @Builder
    private FcmTokenRegisterRequest(String token) {
        this.token = token;
    }

    public static FcmTokenRegisterRequest ofForTest(String token) {
        return FcmTokenRegisterRequest.builder()
                .token(token)
                .build();
    }
}
