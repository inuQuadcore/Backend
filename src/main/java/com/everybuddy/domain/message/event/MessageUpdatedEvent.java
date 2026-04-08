package com.everybuddy.domain.message.event;

import com.everybuddy.domain.message.entity.Message;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MessageUpdatedEvent {
    private final Message message;
    private final Long chatRoomId;

    @Builder
    private MessageUpdatedEvent(Message message, Long chatRoomId) {
        this.message = message;
        this.chatRoomId = chatRoomId;
    }

    public static MessageUpdatedEvent of(Message message, Long chatRoomId) {
        return builder()
                .message(message)
                .chatRoomId(chatRoomId)
                .build();
    }
}
