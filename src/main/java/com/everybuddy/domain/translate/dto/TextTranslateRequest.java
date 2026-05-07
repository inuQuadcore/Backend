package com.everybuddy.domain.translate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class TextTranslateRequest {

    @NotBlank(message = "번역할 텍스트를 입력해주세요.")
    private String text;

    @NotBlank(message = "원본 언어를 입력해주세요.")
    private String sourceLang;

    @NotBlank(message = "목표 언어를 입력해주세요.")
    private String targetLang;
}
