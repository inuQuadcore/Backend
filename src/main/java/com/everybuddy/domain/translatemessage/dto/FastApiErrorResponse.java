package com.everybuddy.domain.translatemessage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FastApiErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;
}
