package com.everybuddy.domain.notification.dto;

import com.everybuddy.domain.notification.entity.Notification;
import com.everybuddy.domain.notification.entity.NotificationType;
import com.everybuddy.global.util.TimeAgoUtil;
import lombok.Builder;
import lombok.Getter;

@Getter
public class NotificationResponse {

    private final Long notificationId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String timeAgo;
    private final boolean isRead;

    @Builder
    private NotificationResponse(Long notificationId, NotificationType type, String title, String body,
                                 String timeAgo, boolean isRead) {
        this.notificationId = notificationId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.timeAgo = timeAgo;
        this.isRead = isRead;
    }

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .timeAgo(TimeAgoUtil.format(notification.getCreatedAt()))
                .isRead(notification.isRead())
                .build();
    }
}
