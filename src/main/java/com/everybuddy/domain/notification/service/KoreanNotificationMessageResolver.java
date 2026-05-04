package com.everybuddy.domain.notification.service;

import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class KoreanNotificationMessageResolver implements NotificationMessageResolver {

    @Override
    public NotificationContent resolveFriendAdded(User fromUser) {
        return NotificationContent.of(
                "새로운 친구",
                fromUser.getName() + "님이 친구로 추가했어요."
        );
    }
}
