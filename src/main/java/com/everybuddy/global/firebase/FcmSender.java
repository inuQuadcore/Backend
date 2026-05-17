package com.everybuddy.global.firebase;

import com.everybuddy.domain.fcmtoken.entity.FcmToken;
import com.everybuddy.domain.fcmtoken.repository.FcmTokenRepository;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmSender {

    private final FcmTokenRepository fcmTokenRepository;

    public void sendToUsers(List<Long> userIds, NotificationContent content, Map<String, String> data) {
        sendToUsers(userIds, content, data, null);
    }

    public void sendToUsers(List<Long> userIds, NotificationContent content, Map<String, String> data,
                            String androidChannelId) {
        if (userIds.isEmpty()) {
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findAllActiveByUserIdIn(userIds);
        if (tokens.isEmpty()) {
            return;
        }

        List<String> tokenValues = tokens.stream().map(FcmToken::getToken).toList();
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(content.getTitle())
                        .setBody(content.getBody())
                        .build())
                .putAllData(data)
                .addAllTokens(tokenValues);

        if (androidChannelId != null) {
            builder.setAndroidConfig(AndroidConfig.builder()
                    .setNotification(AndroidNotification.builder()
                            .setChannelId(androidChannelId)
                            .build())
                    .build());
        }

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
            cleanupInvalidTokens(response, tokens);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 일괄 발송 실패 - userIds={}", userIds, e);
        }
    }

    private void cleanupInvalidTokens(BatchResponse response, List<FcmToken> tokens) {
        List<FcmToken> invalid = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResp = responses.get(i);
            if (sendResp.isSuccessful()) {
                continue;
            }
            MessagingErrorCode errorCode = sendResp.getException().getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                invalid.add(tokens.get(i));
            }
        }
        if (!invalid.isEmpty()) {
            fcmTokenRepository.deleteAll(invalid);
        }
    }
}
