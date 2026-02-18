package com.everybuddy.domain.message.dto;

import com.everybuddy.domain.media.entity.MediaType;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
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
                .lastMessage(getLastMessageDisplay(message))
                .lastMessageTime(message.getSendAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .lastMessageSenderId(message.getUser().getUserId())
                .lastMessageSenderName(message.getUser().getName())
                .build();
    }

    /**
     * 메시지 타입에 따라 적절한 표시 텍스트 반환
     */
    private static String getLastMessageDisplay(Message message) {
        if (message.getMessageType() == MessageType.FILE && message.getMedia() != null) {
            return switch (message.getMedia().getMediaType()) {
                case IMAGE -> "사진을 보냈습니다.";
                case VIDEO -> "동영상을 보냈습니다.";
                case AUDIO -> "음성을 보냈습니다.";
                case DOCUMENT -> "파일을 보냈습니다.";
            };
        }
        return message.getContent();
    }
}
