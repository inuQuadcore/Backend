package com.everybuddy.domain.translate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TextTranslateResponse {

    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
}
