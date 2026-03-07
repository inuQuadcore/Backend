package com.everybuddy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "유저 언어 목록 조회 응답")
public class UserLanguagesResponse {

    @Schema(description = "본인 여부")
    private final boolean isOwner;

    @Schema(description = "언어 목록")
    private final List<UserLanguageResponse> languages;

    @Builder
    private UserLanguagesResponse(boolean isOwner, List<UserLanguageResponse> languages) {
        this.isOwner = isOwner;
        this.languages = languages;
    }

    public static UserLanguagesResponse of(boolean isOwner, List<UserLanguageResponse> languages) {
        return UserLanguagesResponse.builder()
                .isOwner(isOwner)
                .languages(languages)
                .build();
    }
}
