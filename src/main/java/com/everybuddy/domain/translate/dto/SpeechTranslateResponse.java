package com.everybuddy.domain.translate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SpeechTranslateResponse {

    private final String translatedText;

    @Builder
    private SpeechTranslateResponse(String translatedText) {
        this.translatedText = translatedText;
    }

    public static SpeechTranslateResponse of(String translatedText) {
        return SpeechTranslateResponse.builder()
                .translatedText(translatedText)
                .build();
    }
}
