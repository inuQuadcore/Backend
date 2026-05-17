package com.everybuddy.domain.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "상태메시지 답장 인용 프리뷰 (≤30자). 일반 메시지는 null/생략.",
            example = "오늘도 화이팅!")
    @Size(max = 30, message = "상태메시지 프리뷰는 30자 이하여야 합니다.")
    private String statusPreview;

    private ChatMessageRequest() {}

    @Builder
    private ChatMessageRequest(Long chatRoomId, String content, String statusPreview) {
        this.chatRoomId = chatRoomId;
        this.content = content;
        this.statusPreview = statusPreview;
    }

    public static ChatMessageRequest ofForTest(Long chatRoomId, String content) {
        return ofForTest(chatRoomId, content, null);
    }

    public static ChatMessageRequest ofForTest(Long chatRoomId, String content, String statusPreview) {
        return ChatMessageRequest.builder()
                .chatRoomId(chatRoomId)
                .content(content)
                .statusPreview(statusPreview)
                .build();
    }
}
