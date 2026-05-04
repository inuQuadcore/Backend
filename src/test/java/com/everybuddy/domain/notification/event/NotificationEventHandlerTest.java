package com.everybuddy.domain.notification.event;

import com.everybuddy.domain.friendrelation.event.FriendAddedEvent;
import com.everybuddy.domain.notification.service.NotificationService;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventHandler 단위 테스트")
class NotificationEventHandlerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks
    private NotificationEventHandler handler;

    @Test
    @DisplayName("FriendAddedEvent 수신 시 NotificationService.createForFriendAdd 위임")
    void delegatesFriendAddedEvent() {
        User from = User.createForTest(1L, "from", "유저1", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        User to = User.createForTest(2L, "to", "유저2", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1991, 1, 1));
        FriendAddedEvent event = FriendAddedEvent.of(from, to);

        handler.handleFriendAdded(event);

        verify(notificationService).createForFriendAdd(event);
    }
}
