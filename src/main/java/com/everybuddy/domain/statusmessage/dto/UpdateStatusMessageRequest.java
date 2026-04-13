package com.everybuddy.domain.statusmessage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "상태메시지 수정 요청")
public class UpdateStatusMessageRequest {

    @NotBlank
    @Size(max = 100, message = "상태메시지는 100자 이내로 입력해주세요.")
    @Schema(description = "상태메시지 내용", example = "오늘 날씨 너무 좋다!")
    private String content;

    private UpdateStatusMessageRequest() {}

    @Builder
    private UpdateStatusMessageRequest(String content) {
        this.content = content;
    }

    public static UpdateStatusMessageRequest ofForTest(String content) {
        return UpdateStatusMessageRequest.builder()
                .content(content)
                .build();
    }
}
