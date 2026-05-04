package com.everybuddy.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class HasUnreadResponse {

    private final boolean hasUnread;

    @Builder
    private HasUnreadResponse(boolean hasUnread) {
        this.hasUnread = hasUnread;
    }

    public static HasUnreadResponse of(boolean hasUnread) {
        return HasUnreadResponse.builder()
                .hasUnread(hasUnread)
                .build();
    }
}
