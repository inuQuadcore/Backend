package com.everybuddy.domain.message.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateMessageRequest {

    @NotBlank(message = "메시지 본문을 입력해주세요.")
    private String content;

    private UpdateMessageRequest(String content) {
        this.content = content;
    }

    public static UpdateMessageRequest of(String content) {
        return new UpdateMessageRequest(content);
    }
}
