package com.everybuddy.domain.auth.dto;

import com.everybuddy.domain.user.dto.UserLanguageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "회원가입 요청")
public class RegisterRequest {

    @Schema(description = "로그인 Email", example = "babo@gmail.com")
    @NotBlank(message = "ID를 입력해주세요.")
    private String loginId;

    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @Schema(description = "국적", example = "KOREA")
    @NotBlank(message = "국적을 선택해주세요.")
    private String country;

    @Schema(description = "생년월일", example = "2000-01-01")
    @NotBlank(message = "생년월일을 기입해주세요.")
    private String birthday;

    @Schema(description = "성별", example = "MALE")
    @NotBlank(message = "성별을 선택해주세요.")
    private String gender;

    @Schema(description = "자기소개 (최대 150자)", example = "안녕하세요!")
    @Size(max = 150, message = "자기소개는 150자 이내로 입력해주세요.")
    private String bio;

    @Schema(description = "태그 목록", example = "[\"SPORTS\", \"INTJ\"]")
    @NotEmpty(message = "관심 태그를 하나 이상 선택해주세요.")
    private List<String> tags;

    @Schema(description = "관심 언어 목록")
    @NotEmpty(message = "관심 언어를 하나 이상 선택해주세요.")
    private List<UserLanguageRequest> languages;

    @Schema(description = "개인정보 동의 여부", example = "true")
    @AssertTrue(message = "개인정보 동의가 필요합니다.")
    private boolean checked;
}
