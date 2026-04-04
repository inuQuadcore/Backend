package com.everybuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "회원가입 요청")
public class RegisterRequest extends BaseProfileRequest {

    @Schema(description = "로그인 Email", example = "babo@gmail.com")
    @NotBlank(message = "ID를 입력해주세요.")
    private String loginId;

    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @Schema(description = "개인정보 동의 여부", example = "true")
    @AssertTrue(message = "개인정보 동의가 필요합니다.")
    private boolean checked;
}
