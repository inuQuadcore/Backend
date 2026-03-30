package com.everybuddy.domain.discover.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "필터 유저 탐색 응답")
public class FilterDiscoverResponse {

    @Schema(description = "탐색된 유저 목록")
    private final List<DiscoveredUserResponse> users;

    @Schema(description = "다음 페이지 존재 여부")
    private final boolean hasNext;

    @Schema(description = "다음 페이지 커서 (마지막 userId)", example = "50")
    private final Long nextCursor;

    private FilterDiscoverResponse(List<DiscoveredUserResponse> users, boolean hasNext, Long nextCursor) {
        this.users = users;
        this.hasNext = hasNext;
        this.nextCursor = nextCursor;
    }

    public static FilterDiscoverResponse of(List<DiscoveredUserResponse> users, boolean hasNext, Long nextCursor) {
        return new FilterDiscoverResponse(users, hasNext, nextCursor);
    }
}
