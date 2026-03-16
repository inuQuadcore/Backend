package com.everybuddy.domain.friendrelation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "친구 목록 응답")
public class FriendListResponse {

    @Schema(description = "친구 목록")
    private final List<FriendResponse> friends;

    private FriendListResponse(List<FriendResponse> friends) {
        this.friends = friends;
    }

    public static FriendListResponse of(List<FriendResponse> friends) {
        return new FriendListResponse(friends);
    }
}
