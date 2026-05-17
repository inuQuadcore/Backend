package com.everybuddy.domain.friendrelation.dto;

import com.everybuddy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "차단한 유저 정보")
public class BlockedUserResponse {

    @Schema(description = "유저 ID", example = "2")
    private final Long userId;

    @Schema(description = "이름", example = "홍길동")
    private final String name;

    @Schema(description = "프로필 이미지 URL", example = "https://everybuddy.s3.amazonaws.com/profile/2.jpg")
    private final String profileImageUrl;

    @Builder
    private BlockedUserResponse(Long userId, String name, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public static BlockedUserResponse of(User user, String profileImageUrl) {
        return BlockedUserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
