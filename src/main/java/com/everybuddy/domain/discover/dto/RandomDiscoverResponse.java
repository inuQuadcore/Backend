package com.everybuddy.domain.discover.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "랜덤 유저 탐색 응답")
public class RandomDiscoverResponse {

    @Schema(description = "추천 유저 목록")
    private final List<DiscoveredUserResponse> users;

    @Builder
    private RandomDiscoverResponse(List<DiscoveredUserResponse> users) {
        this.users = users;
    }

    public static RandomDiscoverResponse of(List<DiscoveredUserResponse> users) {
        return RandomDiscoverResponse.builder()
                .users(users)
                .build();
    }
}
