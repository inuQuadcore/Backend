package com.everybuddy.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class NotificationListResponse {

    private final List<NotificationResponse> notifications;
    private final Long nextCursor;
    private final boolean hasNext;

    @Builder
    private NotificationListResponse(List<NotificationResponse> notifications, Long nextCursor, boolean hasNext) {
        this.notifications = notifications;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static NotificationListResponse of(List<NotificationResponse> notifications, Long nextCursor, boolean hasNext) {
        return NotificationListResponse.builder()
                .notifications(notifications)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
