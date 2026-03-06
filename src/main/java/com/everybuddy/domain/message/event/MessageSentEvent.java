package com.everybuddy.domain.message.event;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.message.dto.ChatRoomMetadata;
import com.everybuddy.domain.message.entity.Message;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class MessageSentEvent {
    private final Message message;
    private final Long chatRoomId;
    private final List<ChatPart> chatParts;
    private final ChatRoomMetadata metadata;

    @Builder
    private MessageSentEvent(Message message, Long chatRoomId, List<ChatPart> chatParts, ChatRoomMetadata metadata) {
        this.message = message;
        this.chatRoomId = chatRoomId;
        this.chatParts = chatParts;
        this.metadata = metadata;
    }

    public static MessageSentEvent of(Message message, Long chatRoomId, List<ChatPart> chatParts, ChatRoomMetadata metadata) {
        return builder()
                .message(message)
                .chatRoomId(chatRoomId)
                .chatParts(chatParts)
                .metadata(metadata)
                .build();
    }
}
