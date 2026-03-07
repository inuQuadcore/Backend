package com.everybuddy.domain.user.dto;

import com.everybuddy.domain.user.entity.UserTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserTagResponse {

    @Schema(description = "태그")
    private String tag;
    private String category;

    public static UserTagResponse from(UserTag userTag) {
        return new UserTagResponse(
                userTag.getTag().name(),
                userTag.getTag().getCategory().name()
        );
    }
}
