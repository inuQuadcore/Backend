package com.everybuddy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "관심 언어 수준 수정 요청")
public class UserLanguageRequest {

    @Schema(description = "언어 (KOREAN, ENGLISH, JAPANESE, CHINESE, SPANISH, FRENCH, GERMAN)", example = "ENGLISH")
    @NotBlank(message = "언어를 선택해주세요.")
    private String language;

    @Schema(description = "언어 수준 (1~5)", example = "3")
    @NotNull(message = "언어 수준을 선택해주세요.")
    @Min(value = 1, message = "언어 수준은 1 이상이어야 합니다.")
    @Max(value = 5, message = "언어 수준은 5 이하여야 합니다.")
    private Integer level;
}
