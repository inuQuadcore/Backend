package com.everybuddy.domain.message.dto;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.ZoneId;

@Getter
public class FirebaseChatMessage {
    private final Long userId;
    private final String userName;
    private final String messageType;
    private final String content;
    private final Long sendAt;

    @Builder
    private FirebaseChatMessage(Long userId, String userName,
                                String messageType, String content, Long sendAt) {
        this.userId = userId;
        this.userName = userName;
        this.messageType = messageType;
        this.content = content;
        this.sendAt = sendAt;
    }

    public static FirebaseChatMessage from(Message message) {
        return FirebaseChatMessage.builder()
                .userId(message.getUser().getUserId())
                .userName(message.getUser().getName())
                .messageType(message.getMessageType().name())
                .content(message.getContent())
                .sendAt(message.getSendAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build();
    }
}
