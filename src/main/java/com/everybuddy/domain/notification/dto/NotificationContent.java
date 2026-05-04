package com.everybuddy.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class NotificationContent {

    private final String title;
    private final String body;

    @Builder
    private NotificationContent(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public static NotificationContent of(String title, String body) {
        return NotificationContent.builder()
                .title(title)
                .body(body)
                .build();
    }
}
