package com.everybuddy.domain.chatpart.entity;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("ChatPart 엔티티 단위 테스트")
class ChatPartTest {

    private User testUser;
    private ChatRoom testChatRoom;
    private ChatPart chatPart;

    @BeforeEach
    void setUp() {
        testUser = User.createForTest(1L, "testuser", "테스트유저", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        testChatRoom = ChatRoom.createForTest(1L, "테스트 채팅방", true);
        chatPart = ChatPart.create(testUser, testChatRoom);
    }

    private Message messageWithId(Long messageId) {
        return Message.createForTest(messageId, testChatRoom, testUser, MessageType.TEXT,
                "msg-" + messageId, LocalDateTime.now());
    }

    @Nested
    @DisplayName("updateLastReadMessage() - last_read_message_id monotonic 가드")
    class UpdateLastReadMessageGuard {

        @Test
        @DisplayName("TC-1. 최초 호출 (lastReadMessage == null) → 설정됨")
        void firstCallSetsLastReadMessage() {
            // given
            assertNull(chatPart.getLastReadMessage());
            Message message = messageWithId(5L);

            // when
            chatPart.updateLastReadMessage(message);

            // then
            assertAll(
                    () -> assertSame(message, chatPart.getLastReadMessage()),
                    () -> assertNotNull(chatPart.getLastReadAt())
            );
        }

        @Test
        @DisplayName("TC-2. 더 큰 messageId 도착 → lastReadMessage 갱신")
        void advancesWhenLargerMessageIdArrives() {
            // given
            Message earlier = messageWithId(5L);
            chatPart.updateLastReadMessage(earlier);

            Message later = messageWithId(10L);

            // when
            chatPart.updateLastReadMessage(later);

            // then
            assertSame(later, chatPart.getLastReadMessage());
        }

        @ParameterizedTest(name = "TC-3-{index}. 후행 도착 messageId={0} (현재=10) → 무시")
        @ValueSource(longs = {5L, 10L})
        @DisplayName("TC-3. 더 작거나 같은 messageId 도착 → 무시 (기존 값/lastReadAt 유지)")
        void ignoresWhenSmallerOrEqualMessageIdArrives(long laggingMessageId) {
            // given: 현재 last_read_message_id = 10
            Message current = messageWithId(10L);
            chatPart.updateLastReadMessage(current);
            LocalDateTime lastReadAtBefore = chatPart.getLastReadAt();

            Message lagging = messageWithId(laggingMessageId);

            // when
            chatPart.updateLastReadMessage(lagging);

            // then: 인스턴스도 lastReadAt도 그대로
            assertAll(
                    () -> assertSame(current, chatPart.getLastReadMessage()),
                    () -> assertEquals(lastReadAtBefore, chatPart.getLastReadAt())
            );
        }
    }
}
