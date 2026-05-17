package com.everybuddy.domain.message.dto;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MessageResponse 단위 테스트")
class MessageResponseTest {

    private static final ChatRoom CHAT_ROOM = ChatRoom.createForTest(10L, "방", true);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 16, 12, 0);

    @Test
    @DisplayName("TC-1. 활성 유저의 메시지 → userName은 실제 이름")
    void activeUserMessage() {
        User user = User.createForTest(1L, "user1", "홍길동", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        Message message = Message.createForTest(100L, CHAT_ROOM, user, MessageType.TEXT, "안녕", NOW);

        MessageResponse response = MessageResponse.from(message, null);

        assertEquals("홍길동", response.getUserName());
    }

    @Test
    @DisplayName("TC-2. 탈퇴한 유저의 메시지 → userName=\"삭제된 유저\"")
    void deletedUserMessage() {
        User user = User.createForTest(1L, "user1", "홍길동", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        user.softDelete();
        Message message = Message.createForTest(100L, CHAT_ROOM, user, MessageType.TEXT, "안녕", NOW);

        MessageResponse response = MessageResponse.from(message, null);

        assertEquals("삭제된 유저", response.getUserName());
    }
}
