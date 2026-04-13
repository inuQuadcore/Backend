package com.everybuddy.domain.statusmessage.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class FriendStatusMessageListResponse {

    private final List<FriendStatusMessageResponse> statusMessages;
    private final Long nextCursor;
    private final boolean hasNext;

    @Builder
    private FriendStatusMessageListResponse(List<FriendStatusMessageResponse> statusMessages, Long nextCursor, boolean hasNext) {
        this.statusMessages = statusMessages;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static FriendStatusMessageListResponse of(List<FriendStatusMessageResponse> statusMessages, Long nextCursor, boolean hasNext) {
        return FriendStatusMessageListResponse.builder()
                .statusMessages(statusMessages)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
