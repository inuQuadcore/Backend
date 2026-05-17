package com.everybuddy.domain.message.event;

import lombok.Builder;
import lombok.Getter;

@Getter
public class MessageReadEvent {
    private final Long chatRoomId;
    private final Long userId;

    @Builder
    private MessageReadEvent(Long chatRoomId, Long userId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
    }

    public static MessageReadEvent of(Long chatRoomId, Long userId) {
        return builder()
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();
    }
}
