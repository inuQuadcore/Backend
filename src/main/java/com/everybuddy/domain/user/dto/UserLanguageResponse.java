package com.everybuddy.domain.user.dto;

import com.everybuddy.domain.user.entity.UserLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "유저 언어 응답")
public class UserLanguageResponse {

    @Schema(description = "언어", example = "ENGLISH")
    private final String language;

    @Schema(description = "언어 수준 (1~5)", example = "3")
    private final int level;

    @Builder
    private UserLanguageResponse(String language, int level) {
        this.language = language;
        this.level = level;
    }

    public static UserLanguageResponse from(UserLanguage userLanguage) {
        return UserLanguageResponse.builder()
                .language(userLanguage.getLanguage().name())
                .level(userLanguage.getLevel())
                .build();
    }
}
