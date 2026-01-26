package com.everybuddy.domain.message.dto;

import com.everybuddy.domain.message.entity.Message;
import lombok.Builder;
import lombok.Getter;

import java.time.ZoneId;

@Getter
public class ChatRoomMetadata {
    private final Long lastMessageId;
    private final String lastMessage;
    private final Long lastMessageTime;
    private final Long lastMessageSenderId;
    private final String lastMessageSenderName;

    @Builder
    private ChatRoomMetadata(Long lastMessageId, String lastMessage, Long lastMessageTime,
                             Long lastMessageSenderId, String lastMessageSenderName) {
        this.lastMessageId = lastMessageId;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.lastMessageSenderId = lastMessageSenderId;
        this.lastMessageSenderName = lastMessageSenderName;
    }

    public static ChatRoomMetadata from(Message message) {
        return ChatRoomMetadata.builder()
                .lastMessageId(message.getMessageId())
                .lastMessage(message.getContent())
                .lastMessageTime(message.getSendAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .lastMessageSenderId(message.getUser().getUserId())
                .lastMessageSenderName(message.getUser().getName())
                .build();
    }
}
