package com.everybuddy.domain.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "채팅 메시지 전송 요청")
public class ChatMessageRequest {

    @Schema(description = "채팅방 ID", example = "1")
    @NotNull(message = "메시지를 전송할 채팅방을 선택해주세요.")
    private Long chatRoomId;

    @Schema(description = "메시지 내용 (파일 메시지인 경우 생략 가능)", example = "안녕하세요!")
    private String content;

    private ChatMessageRequest() {}

    @Builder
    private ChatMessageRequest(Long chatRoomId, String content) {
        this.chatRoomId = chatRoomId;
        this.content = content;
    }

    public static ChatMessageRequest ofForTest(Long chatRoomId, String content) {
        return ChatMessageRequest.builder()
                .chatRoomId(chatRoomId)
                .content(content)
                .build();
    }
}
