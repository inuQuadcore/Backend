package com.everybuddy.domain.notification.service;

import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("KoreanNotificationMessageResolver 단위 테스트")
class KoreanNotificationMessageResolverTest {

    private final KoreanNotificationMessageResolver resolver = new KoreanNotificationMessageResolver();

    @Test
    @DisplayName("친구추가 알림 본문은 '<이름>님이 친구로 추가했어요.' 형식")
    void resolveFriendAdded() {
        User fromUser = User.createForTest(1L, "user1", "홍길동", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));

        NotificationContent content = resolver.resolveFriendAdded(fromUser);

        assertAll(
                () -> assertEquals("새로운 친구", content.getTitle()),
                () -> assertEquals("홍길동님이 친구로 추가했어요.", content.getBody())
        );
    }
}
