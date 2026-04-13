package com.everybuddy.domain.message.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UpdateMessageRequest {

    @NotBlank(message = "메시지 본문을 입력해주세요.")
    private String content;

    private UpdateMessageRequest() {}

    @Builder
    private UpdateMessageRequest(String content) {
        this.content = content;
    }

    public static UpdateMessageRequest ofForTest(String content) {
        return UpdateMessageRequest.builder()
                .content(content)
                .build();
    }
}
