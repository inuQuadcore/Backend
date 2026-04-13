package com.everybuddy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "태그 수정 요청")
public class UpdateTagsRequest {

    @Schema(description = "태그 목록", example = "[\"SPORTS\", \"INTJ\", \"MOVIES\"]")
    @NotEmpty(message = "태그를 하나 이상 선택해주세요.")
    private List<String> tags;

    private UpdateTagsRequest() {}

    @Builder
    private UpdateTagsRequest(List<String> tags) {
        this.tags = tags;
    }

    public static UpdateTagsRequest ofForTest(List<String> tags) {
        return UpdateTagsRequest.builder()
                .tags(tags)
                .build();
    }
}
