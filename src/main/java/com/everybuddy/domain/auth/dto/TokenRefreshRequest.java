package com.everybuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "토큰 재발급 요청")
public class TokenRefreshRequest {

    @Schema(description = "리프레쉬 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "리프레쉬 토큰은 필수입니다.")
    private String refreshToken;
}
