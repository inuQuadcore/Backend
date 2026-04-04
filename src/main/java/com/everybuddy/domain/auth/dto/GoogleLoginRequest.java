package com.everybuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "Google OAuth 로그인 요청")
public class GoogleLoginRequest {

    @Schema(description = "Google ID Token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "ID Token을 입력해주세요.")
    private String idToken;
}
