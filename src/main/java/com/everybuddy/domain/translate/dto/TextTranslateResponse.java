package com.everybuddy.domain.translate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class TextTranslateResponse {

    private final String translatedText;

    @Builder
    private TextTranslateResponse(String translatedText) {
        this.translatedText = translatedText;
    }

    public static TextTranslateResponse of(String translatedText) {
        return TextTranslateResponse.builder()
                .translatedText(translatedText)
                .build();
    }
}
