package com.everybuddy.domain.translatemessage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FastApiTextRequest {

    @JsonProperty("text")
    private String text;

    @JsonProperty("source_language")
    private String sourceLanguage;

    @JsonProperty("target_language")
    private String targetLanguage;
}
