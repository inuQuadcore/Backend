package com.everybuddy.domain.notification.service;

import com.everybuddy.domain.friendrelation.event.FriendAddedEvent;
import com.everybuddy.domain.notification.dto.HasUnreadResponse;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.notification.dto.NotificationListResponse;
import com.everybuddy.domain.notification.entity.Notification;
import com.everybuddy.domain.notification.entity.NotificationType;
import com.everybuddy.domain.notification.repository.NotificationRepository;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMessageBuilder messageBuilder;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationService notificationService;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        me = User.createForTest(1L, "me", "홍길동", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        other = User.createForTest(2L, "other", "김철수", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1991, 1, 1));
    }

    @Nested
    @DisplayName("getList()")
    class GetList {

        @Test
        @DisplayName("before 없이 호출하면 첫 페이지 조회 (limit+1 조회)")
        void firstPageWhenBeforeIsNull() {
            List<Notification> stored = List.of(
                    notification(30L, "title30"),
                    notification(20L, "title20"),
                    notification(10L, "title10")
            );
            when(notificationRepository.findRecentByRecipientUserId(eq(1L), eq(PageRequest.of(0, 11))))
                    .thenReturn(stored);

            NotificationListResponse response = notificationService.getList(1L, null, 10);

            assertAll(
                    () -> assertEquals(3, response.getNotifications().size()),
                    () -> assertEquals(30L, response.getNotifications().get(0).getNotificationId()),
                    () -> assertEquals(10L, response.getNextCursor()),
                    () -> assertFalse(response.isHasNext())
            );
        }

        @Test
        @DisplayName("before가 주어지면 cursor 기반 조회")
        void usesCursorWhenBeforeProvided() {
            List<Notification> stored = List.of(notification(5L, "title5"));
            when(notificationRepository.findByRecipientUserIdBeforeCursor(eq(1L), eq(10L), eq(PageRequest.of(0, 11))))
                    .thenReturn(stored);

            NotificationListResponse response = notificationService.getList(1L, 10L, 10);

            assertEquals(1, response.getNotifications().size());
            verify(notificationRepository, never()).findRecentByRecipientUserId(eq(1L), eq(PageRequest.of(0, 11)));
        }

        @Test
        @DisplayName("limit+1 만큼 조회되면 hasNext=true, 마지막 1개 잘라냄")
        void hasNextWhenOverLimit() {
            List<Notification> stored = List.of(
                    notification(30L, "n30"),
                    notification(20L, "n20"),
                    notification(10L, "n10")
            );
            when(notificationRepository.findRecentByRecipientUserId(eq(1L), eq(PageRequest.of(0, 3))))
                    .thenReturn(stored);

            NotificationListResponse response = notificationService.getList(1L, null, 2);

            assertAll(
                    () -> assertEquals(2, response.getNotifications().size()),
                    () -> assertEquals(20L, response.getNextCursor()),
                    () -> assertTrue(response.isHasNext())
            );
        }

        @Test
        @DisplayName("결과 없으면 빈 리스트 + nextCursor null + hasNext false")
        void emptyResult() {
            when(notificationRepository.findRecentByRecipientUserId(eq(1L), eq(PageRequest.of(0, 11))))
                    .thenReturn(List.of());

            NotificationListResponse response = notificationService.getList(1L, null, 10);

            assertAll(
                    () -> assertTrue(response.getNotifications().isEmpty()),
                    () -> assertNull(response.getNextCursor()),
                    () -> assertFalse(response.isHasNext())
            );
        }
    }

    @Nested
    @DisplayName("hasUnread()")
    class HasUnread {

        @Test
        @DisplayName("미읽 알림 있으면 true")
        void returnsTrueWhenUnreadExists() {
            when(notificationRepository.existsUnreadByRecipientUserId(1L)).thenReturn(true);

            HasUnreadResponse response = notificationService.hasUnread(1L);

            assertTrue(response.isHasUnread());
        }

        @Test
        @DisplayName("미읽 알림 없으면 false")
        void returnsFalseWhenNoUnread() {
            when(notificationRepository.existsUnreadByRecipientUserId(1L)).thenReturn(false);

            HasUnreadResponse response = notificationService.hasUnread(1L);

            assertFalse(response.isHasUnread());
        }
    }

    @Nested
    @DisplayName("markRead()")
    class MarkRead {

        @Test
        @DisplayName("본인 알림이면 readAt 설정")
        void marksOwnNotificationAsRead() {
            Notification notification = Notification.createForTest(
                    100L, me, NotificationType.FRIEND_ADDED, "t", "b",
                    LocalDateTime.now().minusMinutes(10), null);
            when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));

            notificationService.markRead(1L, 100L);

            assertNotNull(notification.getReadAt());
        }

        @Test
        @DisplayName("이미 읽은 알림이면 readAt 시각을 갱신하지 않음 (멱등)")
        void doesNotOverwriteAlreadyRead() {
            LocalDateTime previouslyReadAt = LocalDateTime.now().minusMinutes(5);
            Notification notification = Notification.createForTest(
                    100L, me, NotificationType.FRIEND_ADDED, "t", "b",
                    LocalDateTime.now().minusMinutes(10), previouslyReadAt);
            when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));

            notificationService.markRead(1L, 100L);

            assertEquals(previouslyReadAt, notification.getReadAt());
        }

        @Test
        @DisplayName("타인의 알림이면 NOTIFICATION_NOT_FOUND")
        void throwsWhenNotOwnNotification() {
            Notification notification = Notification.createForTest(
                    100L, other, NotificationType.FRIEND_ADDED, "t", "b",
                    LocalDateTime.now().minusMinutes(10), null);
            when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));

            CustomException exception = assertThrows(CustomException.class,
                    () -> notificationService.markRead(1L, 100L));

            assertAll(
                    () -> assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode()),
                    () -> assertNull(notification.getReadAt())
            );
        }

        @Test
        @DisplayName("알림 없으면 NOTIFICATION_NOT_FOUND")
        void throwsWhenNotificationNotFound() {
            when(notificationRepository.findById(100L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> notificationService.markRead(1L, 100L));

            assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("createForFriendAdd()")
    class CreateForFriendAdd {

        @Test
        @DisplayName("Notification 저장 후 saved 반환 (수신자=toUser, 본문=builder 결과)")
        void savesAndReturnsSaved() {
            NotificationContent content = NotificationContent.of("새로운 친구", "홍길동님이 친구로 추가했어요.");
            when(messageBuilder.resolveFriendAdded(me)).thenReturn(content);
            when(notificationRepository.save(org.mockito.ArgumentMatchers.any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            FriendAddedEvent event = FriendAddedEvent.of(me, other);
            Notification saved = notificationService.createForFriendAdd(event);

            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();
            assertAll(
                    () -> assertEquals(2L, captured.getRecipient().getUserId()),
                    () -> assertEquals(NotificationType.FRIEND_ADDED, captured.getType()),
                    () -> assertEquals("새로운 친구", captured.getTitle()),
                    () -> assertEquals("홍길동님이 친구로 추가했어요.", captured.getBody()),
                    () -> assertTrue(captured.getPayload().contains("\"fromUserId\":1")),
                    () -> assertEquals(captured, saved)
            );
        }
    }

    @Nested
    @DisplayName("markAllRead()")
    class MarkAllRead {

        @Test
        @DisplayName("repository.markAllReadByRecipientUserId 호출")
        void delegatesToRepository() {
            notificationService.markAllRead(1L);

            verify(notificationRepository).markAllReadByRecipientUserId(eq(1L), org.mockito.ArgumentMatchers.any(LocalDateTime.class));
        }
    }

    private Notification notification(Long id, String title) {
        return Notification.createForTest(id, me, NotificationType.FRIEND_ADDED, title, "body",
                LocalDateTime.now().minusMinutes(id), null);
    }
}
