package com.everybuddy.domain.chatroom.dto;

import com.everybuddy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "채팅방 참여자 정보")
public class ChatRoomParticipantResponse {

    @Schema(description = "유저 ID", example = "2")
    private final Long userId;

    @Schema(description = "이름", example = "홍길동")
    private final String name;

    @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://everybuddy.s3.amazonaws.com/profile/2.jpg")
    private final String profileImageUrl;

    @Builder
    private ChatRoomParticipantResponse(Long userId, String name, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public static ChatRoomParticipantResponse of(User user, String profileImageUrl) {
        return ChatRoomParticipantResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
