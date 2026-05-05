package com.everybuddy.domain.notification.service;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.user.entity.User;

public interface NotificationMessageResolver {

    NotificationContent resolveFriendAdded(User fromUser);

    NotificationContent resolveChatMessage(Message message);
}
