package com.everybuddy.domain.notification.service;

import com.everybuddy.domain.friendrelation.event.FriendAddedEvent;
import com.everybuddy.domain.notification.dto.HasUnreadResponse;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.notification.dto.NotificationListResponse;
import com.everybuddy.domain.notification.dto.NotificationResponse;
import com.everybuddy.domain.notification.entity.Notification;
import com.everybuddy.domain.notification.entity.NotificationType;
import com.everybuddy.domain.notification.repository.NotificationRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.firebase.FcmSender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final FcmSender fcmSender;
    private final ObjectMapper objectMapper;

    public void createForFriendAdd(FriendAddedEvent event) {
        NotificationContent content = messageBuilder.resolveFriendAdded(event.getFromUser());
        String payload = serializePayload(Map.of("fromUserId", event.getFromUser().getUserId()));

        notificationRepository.save(Notification.of(
                event.getToUser(),
                NotificationType.FRIEND_ADDED,
                content.getTitle(),
                content.getBody(),
                payload
        ));

        Map<String, String> data = Map.of(
                "type", NotificationType.FRIEND_ADDED.name(),
                "fromUserId", String.valueOf(event.getFromUser().getUserId())
        );
        fcmSender.sendToUsers(List.of(event.getToUser().getUserId()), content, data);
    }

    private String serializePayload(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("알림 payload 직렬화 실패 - map={}", map, e);
            return "{}";
        }
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getList(Long userId, Long before, int limit) {
        List<Notification> results = fetchNotifications(userId, before, limit);
        return toPagedResponse(results, limit);
    }

    private NotificationListResponse toPagedResponse(List<Notification> results, int limit) {
        boolean hasNext = results.size() > limit;
        List<Notification> page = hasNext ? results.subList(0, limit) : results;
        Long nextCursor = page.isEmpty() ? null : page.getLast().getNotificationId();
        List<NotificationResponse> responses = page.stream()
                .map(NotificationResponse::from)
                .toList();
        return NotificationListResponse.of(responses, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public HasUnreadResponse hasUnread(Long userId) {
        return HasUnreadResponse.of(notificationRepository.existsUnreadByRecipientUserId(userId));
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        notification.markAsRead();
    }

    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByRecipientUserId(userId, LocalDateTime.now());
    }

    private List<Notification> fetchNotifications(Long userId, Long before, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        if (before == null) {
            return notificationRepository.findRecentByRecipientUserId(userId, pageRequest);
        }
        return notificationRepository.findByRecipientUserIdBeforeCursor(userId, before, pageRequest);
    }
}
