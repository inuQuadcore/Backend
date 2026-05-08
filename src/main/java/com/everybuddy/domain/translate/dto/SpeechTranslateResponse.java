package com.everybuddy.domain.translate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SpeechTranslateResponse {

    private final String sourceText;
    private final String translatedText;
    private final String sourceLanguage;
    private final String targetLanguage;

    @Builder
    private SpeechTranslateResponse(String sourceText, String translatedText,
                                    String sourceLanguage, String targetLanguage) {
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    public static SpeechTranslateResponse of(String sourceText, String translatedText,
                                             String sourceLanguage, String targetLanguage) {
        return SpeechTranslateResponse.builder()
                .sourceText(sourceText)
                .translatedText(translatedText)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .build();
    }
}
