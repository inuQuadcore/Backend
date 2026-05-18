package com.everybuddy.domain.notification.event;

import com.everybuddy.domain.friendrelation.event.FriendAddedEvent;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.notification.service.NotificationMessageBuilder;
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

    private final NotificationMessageBuilder messageBuilder;
    private final FcmSender fcmSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendAdded(FriendAddedEvent event) {
        NotificationContent content = messageBuilder.resolveFriendAdded(event.getFromUser());
        Map<String, String> data = Map.of(
                "fromUserId", String.valueOf(event.getFromUser().getUserId())
        );
        fcmSender.sendToUsers(List.of(event.getToUser().getUserId()), content, data);
    }
}
