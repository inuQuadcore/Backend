package com.everybuddy.domain.translatemessage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FastApiTextResponse {

    @JsonProperty("translated_text")
    private String translatedText;
}
