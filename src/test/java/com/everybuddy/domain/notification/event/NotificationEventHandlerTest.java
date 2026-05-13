package com.everybuddy.domain.notification.event;

import com.everybuddy.domain.friendrelation.event.FriendAddedEvent;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.notification.service.NotificationService;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.global.firebase.FcmSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventHandler 단위 테스트")
class NotificationEventHandlerTest {

    @Mock private NotificationService notificationService;
    @Mock private FcmSender fcmSender;

    @InjectMocks
    private NotificationEventHandler handler;

    @Test
    @DisplayName("FriendAddedEvent 수신 시 createForFriendAdd 위임 후 반환된 content로 FCM 발송")
    void delegatesAndSendsFcm() {
        User from = User.createForTest(1L, "from", "유저1", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        User to = User.createForTest(2L, "to", "유저2", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1991, 1, 1));
        FriendAddedEvent event = FriendAddedEvent.of(from, to);

        NotificationContent content = NotificationContent.of("새로운 친구", "유저1님이 친구로 추가했어요.");
        when(notificationService.createForFriendAdd(event)).thenReturn(content);

        handler.handleFriendAdded(event);

        verify(notificationService).createForFriendAdd(event);

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmSender).sendToUsers(eq(List.of(2L)), eq(content), dataCaptor.capture());
        assertEquals("1", dataCaptor.getValue().get("fromUserId"));
    }
}
