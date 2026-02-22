package com.everybuddy.domain.message.event;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.message.entity.Message;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class MessageDeletedEvent {
    private final Message message;
    private final Long chatRoomId;
    private final boolean last;
    private final List<ChatPart> chatParts;

    @Builder
    private MessageDeletedEvent(Message message, Long chatRoomId, boolean last, List<ChatPart> chatParts) {
        this.message = message;
        this.chatRoomId = chatRoomId;
        this.last = last;
        this.chatParts = chatParts;
    }

    public static MessageDeletedEvent of(Message message, Long chatRoomId, boolean last, List<ChatPart> chatParts) {
        return builder()
                .message(message)
                .chatRoomId(chatRoomId)
                .last(last)
                .chatParts(chatParts)
                .build();
    }
}
