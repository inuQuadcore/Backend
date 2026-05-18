package com.everybuddy.domain.notification.service;

import com.everybuddy.domain.notification.dto.HasUnreadResponse;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.notification.dto.NotificationListResponse;
import com.everybuddy.domain.notification.dto.NotificationResponse;
import com.everybuddy.domain.notification.entity.Notification;
import com.everybuddy.domain.notification.repository.NotificationRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMessageBuilder messageBuilder;

    public void createForFriendAdd(User fromUser, User toUser) {
        NotificationContent content = messageBuilder.resolveFriendAdded(fromUser);
        notificationRepository.save(Notification.of(toUser, fromUser, content.getBody()));
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getList(Long userId, Long before, int limit) {
        List<Notification> results = fetchNotifications(userId, before, limit);
        return toPagedResponse(results, limit);
    }

    @Transactional(readOnly = true)
    public HasUnreadResponse hasUnread(Long userId) {
        return HasUnreadResponse.of(notificationRepository.existsUnreadByRecipientUserId(userId));
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.NOT_NOTIFICATION_OF_USER);
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

    private NotificationListResponse toPagedResponse(List<Notification> results, int limit) {
        boolean hasNext = results.size() > limit;
        List<Notification> page = hasNext ? results.subList(0, limit) : results;
        Long nextCursor = page.isEmpty() ? null : page.getLast().getNotificationId();
        List<NotificationResponse> responses = page.stream()
                .map(NotificationResponse::from)
                .toList();
        return NotificationListResponse.of(responses, nextCursor, hasNext);
    }
}
