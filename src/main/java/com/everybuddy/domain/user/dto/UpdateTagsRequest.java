package com.everybuddy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "태그 수정 요청")
public class UpdateTagsRequest {

    @Schema(description = "태그 목록 (빈 리스트 전송 시 전체 삭제)", example = "[\"SPORTS\", \"INTJ\", \"MOVIES\"]")
    @NotNull(message = "태그 목록을 입력해주세요.")
    private List<String> tags;

    public static UpdateTagsRequest of(List<String> tags) {
        UpdateTagsRequest request = new UpdateTagsRequest();
        request.tags = tags;
        return request;
    }
}
