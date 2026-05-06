package com.everybuddy.global.firebase;

import com.everybuddy.domain.message.event.MessageSentEvent;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.notification.service.NotificationMessageBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatPushEventHandler {

    private static final String DATA_TYPE_CHAT_MESSAGE = "CHAT_MESSAGE";

    private final ViewingSyncService viewingSyncService;
    private final NotificationMessageBuilder messageBuilder;
    private final FcmSender fcmSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageSent(MessageSentEvent event) {
        Long senderId = event.getMessage().getUser().getUserId();
        Long chatRoomId = event.getChatRoomId();

        List<Long> targetUserIds = event.getChatParts().stream()
                .map(chatPart -> chatPart.getUser().getUserId())
                .filter(userId -> !userId.equals(senderId))
                .filter(userId -> !viewingSyncService.isViewing(userId, chatRoomId))
                .toList();

        if (targetUserIds.isEmpty()) {
            return;
        }

        NotificationContent content = messageBuilder.resolveChatMessage(event.getMessage());
        Map<String, String> data = Map.of(
                "type", DATA_TYPE_CHAT_MESSAGE,
                "chatRoomId", String.valueOf(chatRoomId),
                "messageId", String.valueOf(event.getMessage().getMessageId()),
                "senderId", String.valueOf(senderId)
        );

        fcmSender.sendToUsers(targetUserIds, content, data);
    }
}
