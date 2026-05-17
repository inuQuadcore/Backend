package com.everybuddy.domain.friendrelation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "차단 목록 응답")
public class BlockedUsersResponse {

    @Schema(description = "차단한 유저 목록 (최근 차단순)")
    private final List<BlockedUserResponse> blockedUsers;

    @Builder
    private BlockedUsersResponse(List<BlockedUserResponse> blockedUsers) {
        this.blockedUsers = blockedUsers;
    }

    public static BlockedUsersResponse of(List<BlockedUserResponse> blockedUsers) {
        return BlockedUsersResponse.builder()
                .blockedUsers(blockedUsers)
                .build();
    }
}
