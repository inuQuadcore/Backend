package com.everybuddy.domain.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class RegisterRequest {
    @NotBlank(message = "ID를 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "국적을 선택해주세요.")
    private String country;

    @NotBlank(message = "생년월일을 기입해주세요.")
    private String birthday;

    @NotBlank(message = "성별을 선택해주세요.")
    private String gender;

    // 서비스 약관 동의여부, 검증을 위해 둔 필드
    @AssertTrue(message = "개인정보 동의가 필요합니다.")
    private boolean checked;
}
