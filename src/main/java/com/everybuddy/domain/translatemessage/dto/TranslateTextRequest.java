package com.everybuddy.domain.translatemessage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TranslateTextRequest {

    @NotBlank
    private String text;

    @NotBlank
    private String sourceLanguage;

    @NotBlank
    private String targetLanguage;
}
