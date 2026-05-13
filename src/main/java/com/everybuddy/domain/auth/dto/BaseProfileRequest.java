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
public abstract class BaseProfileRequest {

    @Schema(description = "개인정보 동의 여부", example = "true")
    @AssertTrue(message = "개인정보 동의가 필요합니다.")
    private boolean checked;

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

    @Schema(description = "태그 목록", example = "[\"WORKOUT\", \"INTJ\"]")
    @NotEmpty(message = "관심 태그를 하나 이상 선택해주세요.")
    private List<String> tags;

    @Schema(description = "주 언어 (번역 기본 대상 언어, 자동으로 level 5 처리)", example = "KOREAN")
    @NotBlank(message = "주 언어를 선택해주세요.")
    private String primaryLanguage;

    @Schema(description = "관심 언어 목록")
    @NotEmpty(message = "관심 언어를 하나 이상 선택해주세요.")
    private List<UserLanguageRequest> interestLanguages;
}
