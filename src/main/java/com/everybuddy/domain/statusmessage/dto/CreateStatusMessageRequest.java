package com.everybuddy.domain.statusmessage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "상태메시지 작성 요청")
public class CreateStatusMessageRequest {

    @NotBlank
    @Size(max = 100, message = "상태메시지는 100자 이내로 입력해주세요.")
    @Schema(description = "상태메시지 내용", example = "오늘 날씨 너무 좋다!")
    private String content;

    private CreateStatusMessageRequest() {}

    @Builder
    private CreateStatusMessageRequest(String content) {
        this.content = content;
    }

    public static CreateStatusMessageRequest ofForTest(String content) {
        return CreateStatusMessageRequest.builder()
                .content(content)
                .build();
    }
}
