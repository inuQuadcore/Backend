package com.everybuddy.domain.notification.service;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.entity.MediaType;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("KoreanNotificationMessageResolver 단위 테스트")
class KoreanNotificationMessageResolverTest {

    private final KoreanNotificationMessageResolver resolver = new KoreanNotificationMessageResolver();

    private final User sender = User.createForTest(1L, "user1", "홍길동", "password",
            Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
    private final ChatRoom chatRoom = ChatRoom.createForTest(100L, "테스트방");

    @Test
    @DisplayName("친구추가 알림 본문은 '<이름>님이 친구로 추가했어요.' 형식")
    void resolveFriendAdded() {
        NotificationContent content = resolver.resolveFriendAdded(sender);

        assertAll(
                () -> assertEquals("새로운 친구", content.getTitle()),
                () -> assertEquals("홍길동님이 친구로 추가했어요.", content.getBody())
        );
    }

    @Test
    @DisplayName("TEXT 채팅 메시지 본문은 메시지 content 그대로")
    void resolveChatMessageText() {
        Message message = Message.createForTest(500L, chatRoom, sender, MessageType.TEXT, "안녕하세요",
                LocalDateTime.of(2026, 5, 5, 10, 0));

        NotificationContent content = resolver.resolveChatMessage(message);

        assertAll(
                () -> assertEquals("홍길동", content.getTitle()),
                () -> assertEquals("안녕하세요", content.getBody())
        );
    }

    @ParameterizedTest(name = "{0} 미디어 → 본문 \"{1}\"")
    @MethodSource("mediaTypeBodyProvider")
    @DisplayName("FILE 채팅 메시지 본문은 미디어 타입별로 분기된다")
    void resolveChatMessageFile(MediaType mediaType, String expectedBody) {
        Media media = Media.createForTest(mediaType);
        Message message = Message.createWithMediaForTest(500L, chatRoom, sender, media, MessageType.FILE,
                LocalDateTime.of(2026, 5, 5, 10, 0));

        NotificationContent content = resolver.resolveChatMessage(message);

        assertAll(
                () -> assertEquals("홍길동", content.getTitle()),
                () -> assertEquals(expectedBody, content.getBody())
        );
    }

    private static Stream<Arguments> mediaTypeBodyProvider() {
        return Stream.of(
                Arguments.of(MediaType.IMAGE, "📷 사진"),
                Arguments.of(MediaType.VIDEO, "🎥 동영상"),
                Arguments.of(MediaType.AUDIO, "🎤 음성"),
                Arguments.of(MediaType.DOCUMENT, "📎 파일")
        );
    }
}
