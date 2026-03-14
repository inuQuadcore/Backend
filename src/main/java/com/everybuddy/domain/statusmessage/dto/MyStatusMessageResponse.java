package com.everybuddy.domain.statusmessage.dto;

import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import com.everybuddy.global.util.TimeAgoUtil;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MyStatusMessageResponse {

    private final Long statusMessageId;
    private final String profileImageUrl;
    private final String nickname;
    private final String content;
    private final String timeAgo;

    @Builder
    private MyStatusMessageResponse(Long statusMessageId, String profileImageUrl, String nickname, String content, String timeAgo) {
        this.statusMessageId = statusMessageId;
        this.profileImageUrl = profileImageUrl;
        this.nickname = nickname;
        this.content = content;
        this.timeAgo = timeAgo;
    }

    public static MyStatusMessageResponse from(StatusMessage statusMessage, String profileImageUrl) {
        return MyStatusMessageResponse.builder()
                .statusMessageId(statusMessage.getStatusMessageId())
                .profileImageUrl(profileImageUrl)
                .nickname(statusMessage.getUser().getName())
                .content(statusMessage.getContent())
                .timeAgo(TimeAgoUtil.format(statusMessage.getUpdatedAt()))
                .build();
    }

    public static MyStatusMessageResponse of(Long statusMessageId, String profileImageUrl, String nickname, String content, String timeAgo) {
        return MyStatusMessageResponse.builder()
                .statusMessageId(statusMessageId)
                .profileImageUrl(profileImageUrl)
                .nickname(nickname)
                .content(content)
                .timeAgo(timeAgo)
                .build();
    }
}
