package com.everybuddy.domain.statusmessage.dto;

import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyStatusMessageResponse {

    private final Long statusMessageId;
    private final String profileImageUrl;
    private final String nickname;
    private final String content;
    private final LocalDateTime updatedAt;

    @Builder
    private MyStatusMessageResponse(Long statusMessageId, String profileImageUrl, String nickname, String content, LocalDateTime updatedAt) {
        this.statusMessageId = statusMessageId;
        this.profileImageUrl = profileImageUrl;
        this.nickname = nickname;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public static MyStatusMessageResponse from(StatusMessage statusMessage, String profileImageUrl) {
        return MyStatusMessageResponse.builder()
                .statusMessageId(statusMessage.getStatusMessageId())
                .profileImageUrl(profileImageUrl)
                .nickname(statusMessage.getUser().getName())
                .content(statusMessage.getContent())
                .updatedAt(statusMessage.getUpdatedAt())
                .build();
    }

}
