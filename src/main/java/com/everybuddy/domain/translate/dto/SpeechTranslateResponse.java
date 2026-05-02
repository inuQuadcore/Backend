package com.everybuddy.domain.translate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpeechTranslateResponse {

    private String sourceText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
}
