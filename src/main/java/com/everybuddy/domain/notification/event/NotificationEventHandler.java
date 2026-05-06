package com.everybuddy.domain.notification.event;

import com.everybuddy.domain.friendrelation.event.FriendAddedEvent;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.notification.entity.Notification;
import com.everybuddy.domain.notification.entity.NotificationType;
import com.everybuddy.domain.notification.service.NotificationService;
import com.everybuddy.global.firebase.FcmSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationService notificationService;
    private final FcmSender fcmSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendAdded(FriendAddedEvent event) {
        Notification saved = notificationService.createForFriendAdd(event);

        NotificationContent content = NotificationContent.of(saved.getTitle(), saved.getBody());
        Map<String, String> data = Map.of(
                "type", NotificationType.FRIEND_ADDED.name(),
                "fromUserId", String.valueOf(event.getFromUser().getUserId())
        );
        fcmSender.sendToUsers(List.of(event.getToUser().getUserId()), content, data);
    }
}
