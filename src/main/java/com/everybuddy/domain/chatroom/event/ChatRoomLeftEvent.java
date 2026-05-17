package com.everybuddy.domain.chatroom.event;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ChatRoomLeftEvent {
    private final Long chatRoomId;
    private final Long userId;

    @Builder
    private ChatRoomLeftEvent(Long chatRoomId, Long userId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
    }

    public static ChatRoomLeftEvent of(Long chatRoomId, Long userId) {
        return builder()
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();
    }
}
