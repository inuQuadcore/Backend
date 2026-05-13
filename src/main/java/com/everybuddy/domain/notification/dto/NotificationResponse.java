package com.everybuddy.domain.notification.dto;

import com.everybuddy.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {

    private final Long notificationId;
    private final String body;
    private final Long fromUserId;
    private final LocalDateTime createdAt;
    private final boolean isRead;

    @Builder
    private NotificationResponse(Long notificationId, String body, Long fromUserId,
                                 LocalDateTime createdAt, boolean isRead) {
        this.notificationId = notificationId;
        this.body = body;
        this.fromUserId = fromUserId;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .body(notification.getBody())
                .fromUserId(notification.getFromUser() != null ? notification.getFromUser().getUserId() : null)
                .createdAt(notification.getCreatedAt())
                .isRead(notification.isRead())
                .build();
    }
}
