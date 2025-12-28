package com.everybuddy.domain.message.dto;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {
    private final Long messageId;
    private final Long chatRoomId;
    private final Long userId;
    private final String userName;
    private final MessageType messageType;
    private final String content;
    private final LocalDateTime sendAt;

    @Builder
    private ChatMessageResponse(Long messageId, Long chatRoomId, Long userId, String userName,
                                MessageType messageType, String content, LocalDateTime sendAt) {
        this.messageId = messageId;
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.userName = userName;
        this.messageType = messageType;
        this.content = content;
        this.sendAt = sendAt;
    }

    public static ChatMessageResponse from(Message message) {
        return ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .chatRoomId(message.getChatRoom().getChatRoomId())
                .userId(message.getUser().getUserId())
                .userName(message.getUser().getName())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .sendAt(message.getSendAt())
                .build();
    }
}
