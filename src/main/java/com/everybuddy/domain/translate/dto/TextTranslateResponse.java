package com.everybuddy.domain.translate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class TextTranslateResponse {

    private final String translatedText;
    private final String sourceLanguage;
    private final String targetLanguage;

    @Builder
    private TextTranslateResponse(String translatedText, String sourceLanguage, String targetLanguage) {
        this.translatedText = translatedText;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    public static TextTranslateResponse of(String translatedText, String sourceLanguage, String targetLanguage) {
        return TextTranslateResponse.builder()
                .translatedText(translatedText)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .build();
    }
}
