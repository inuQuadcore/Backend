package com.everybuddy.domain.translate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class TtsRequest {

    @NotBlank(message = "변환할 텍스트를 입력해주세요.")
    @Size(max = 500, message = "텍스트는 최대 500자까지 입력할 수 있습니다.")
    private String text;

    private String language;

    private String voice;
}
