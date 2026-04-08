package com.everybuddy.domain.translatemessage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TranslateTextResponse {

    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
}
