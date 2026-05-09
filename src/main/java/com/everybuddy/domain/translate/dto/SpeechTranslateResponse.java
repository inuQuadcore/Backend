package com.everybuddy.domain.translate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SpeechTranslateResponse {

    private final String sourceText;
    private final String translatedText;

    @Builder
    private SpeechTranslateResponse(String sourceText, String translatedText) {
        this.sourceText = sourceText;
        this.translatedText = translatedText;
    }

    public static SpeechTranslateResponse of(String sourceText, String translatedText) {
        return SpeechTranslateResponse.builder()
                .sourceText(sourceText)
                .translatedText(translatedText)
                .build();
    }
}
