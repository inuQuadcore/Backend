package com.everybuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "Google OAuth 회원가입 요청")
public class GoogleRegisterRequest extends BaseProfileRequest {

    @Schema(description = "1단계에서 발급받은 임시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "임시 토큰을 입력해주세요.")
    private String tempToken;
}
