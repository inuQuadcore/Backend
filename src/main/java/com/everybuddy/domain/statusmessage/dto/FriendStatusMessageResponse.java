package com.everybuddy.domain.statusmessage.dto;

import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FriendStatusMessageResponse {

    private final Long statusMessageId;
    private final String profileImageUrl;
    private final String nickname;
    private final String content;
    private final LocalDateTime updatedAt;

    @Builder
    private FriendStatusMessageResponse(Long statusMessageId, String profileImageUrl, String nickname, String content, LocalDateTime updatedAt) {
        this.statusMessageId = statusMessageId;
        this.profileImageUrl = profileImageUrl;
        this.nickname = nickname;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public static FriendStatusMessageResponse from(StatusMessage statusMessage, String profileImageUrl) {
        return FriendStatusMessageResponse.builder()
                .statusMessageId(statusMessage.getStatusMessageId())
                .profileImageUrl(profileImageUrl)
                .nickname(statusMessage.getUser().getName())
                .content(statusMessage.getContent())
                .updatedAt(statusMessage.getUpdatedAt())
                .build();
    }

}
