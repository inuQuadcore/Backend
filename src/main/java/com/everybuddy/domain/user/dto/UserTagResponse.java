package com.everybuddy.domain.user.dto;

import com.everybuddy.domain.user.entity.UserTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "유저 태그 응답")
public class UserTagResponse {

    @Schema(description = "태그", example = "WORKOUT")
    private final String tag;

    @Schema(description = "태그 카테고리", example = "HOBBY")
    private final String category;

    @Builder
    private UserTagResponse(String tag, String category) {
        this.tag = tag;
        this.category = category;
    }

    public static UserTagResponse from(UserTag userTag) {
        return UserTagResponse.builder()
                .tag(userTag.getTag().name())
                .category(userTag.getTag().getCategory().name())
                .build();
    }
}
