package com.everybuddy.domain.message.dto;

import com.everybuddy.domain.message.entity.MessageType;
import lombok.Getter;

@Getter
public class ChatMessageRequest {
    private Long chatRoomId;
    private MessageType messageType;
    private String content;
}
