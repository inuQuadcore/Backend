package com.everybuddy.domain.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChatMessageRequest {
    @NotNull(message = "메시지를 전송할 채팅방을 선택해주세요.")
    private Long chatRoomId;

    @NotBlank(message = "메시지 타입을 확인해주세요.")
    private String messageType;

    @NotBlank(message = "메시지 본문을 입력해주세요.")
    private String content;
}
