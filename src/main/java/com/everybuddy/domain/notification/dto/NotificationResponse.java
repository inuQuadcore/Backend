package com.everybuddy.domain.notification.dto;

import com.everybuddy.domain.notification.entity.Notification;
import com.everybuddy.global.util.TimeAgoUtil;
import lombok.Builder;
import lombok.Getter;

@Getter
public class NotificationResponse {

    private final Long notificationId;
    private final String body;
    private final Long fromUserId;
    private final String timeAgo;
    private final boolean isRead;

    @Builder
    private NotificationResponse(Long notificationId, String body, Long fromUserId,
                                 String timeAgo, boolean isRead) {
        this.notificationId = notificationId;
        this.body = body;
        this.fromUserId = fromUserId;
        this.timeAgo = timeAgo;
        this.isRead = isRead;
    }

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .body(notification.getBody())
                .fromUserId(notification.getFromUser() != null ? notification.getFromUser().getUserId() : null)
                .timeAgo(TimeAgoUtil.format(notification.getCreatedAt()))
                .isRead(notification.isRead())
                .build();
    }
}
