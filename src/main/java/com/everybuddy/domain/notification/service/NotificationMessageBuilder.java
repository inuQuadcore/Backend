package com.everybuddy.domain.notification.service;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageBuilder {

    public NotificationContent resolveFriendAdded(User fromUser) {
        return NotificationContent.of(
                "새로운 친구",
                fromUser.getName() + "님이 친구로 추가했어요."
        );
    }

    public NotificationContent resolveChatMessage(Message message) {
        return NotificationContent.of(
                message.getUser().getName(),
                buildChatBody(message)
        );
    }

    private String buildChatBody(Message message) {
        if (message.getMessageType() == MessageType.FILE && message.getMedia() != null) {
            return switch (message.getMedia().getMediaType()) {
                case IMAGE -> "📷 사진";
                case VIDEO -> "🎥 동영상";
                case AUDIO -> "🎤 음성";
                case DOCUMENT -> "📎 파일";
            };
        }
        return message.getContent();
    }
}
