package com.everybuddy.domain.translate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class TextTranslateRequest {

    @NotBlank(message = "번역할 텍스트를 입력해주세요.")
    private String text;

    public static TextTranslateRequest ofForTest(String text) {
        TextTranslateRequest request = new TextTranslateRequest();
        request.text = text;
        return request;
    }
}
