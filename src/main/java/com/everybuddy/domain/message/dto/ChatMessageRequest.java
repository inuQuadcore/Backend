package com.everybuddy.domain.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "채팅 메시지 전송 요청")
public class ChatMessageRequest {

    @Schema(description = "채팅방 ID", example = "1")
    @NotNull(message = "메시지를 전송할 채팅방을 선택해주세요.")
    private Long chatRoomId;

    @Schema(description = "메시지 타입 (text, image, file)", example = "text")
    @NotBlank(message = "메시지 타입을 확인해주세요.")
    private String messageType;

    @Schema(description = "메시지 내용", example = "안녕하세요!")
    @NotBlank(message = "메시지 본문을 입력해주세요.")
    private String content;
}
