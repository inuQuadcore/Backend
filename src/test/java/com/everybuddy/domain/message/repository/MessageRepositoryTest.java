package com.everybuddy.domain.message.repository;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@DisplayName("MessageRepository 슬라이스 테스트")
class MessageRepositoryTest {

    private static final LocalDateTime LONG_AGO = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Autowired private MessageRepository messageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private TestEntityManager em;

    private User user;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.createForTest(null, "loginA", "A", "pw",
                Country.KOREA, Gender.MALE, LocalDate.of(2000, 1, 1)));
        chatRoom = chatRoomRepository.save(ChatRoom.create("test room"));
    }

    private Message saveMessage(String content) {
        return messageRepository.save(Message.create(chatRoom, user, MessageType.TEXT, content));
    }

    /**
     * sendAt은 @CreatedDate로 자동 세팅되므로, 테스트에서 과거 시점 메시지가 필요한 경우
     * 저장 후 native SQL로 직접 세팅한다.
     */
    private void setSendAtInPast(Long messageId, LocalDateTime sendAt) {
        em.getEntityManager()
                .createNativeQuery("UPDATE message SET send_at = :sendAt WHERE message_id = :id")
                .setParameter("sendAt", sendAt)
                .setParameter("id", messageId)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("countUnreadMessages")
    class CountUnreadMessagesTest {

        @Test
        @DisplayName("lastReadMessageId가 null이면 삭제되지 않은 전체 메시지 수 반환")
        void nullLastReadMessageId() {
            saveMessage("msg1");
            saveMessage("msg2");

            Long count = messageRepository.countUnreadMessages(chatRoom.getChatRoomId(), null, LONG_AGO);

            assertEquals(2L, count);
        }

        @Test
        @DisplayName("lastReadMessageId보다 큰 messageId를 가진 메시지만 카운트")
        void withLastReadMessageId() {
            Message m1 = saveMessage("msg1");
            saveMessage("msg2");
            saveMessage("msg3");

            Long count = messageRepository.countUnreadMessages(chatRoom.getChatRoomId(), m1.getMessageId(), LONG_AGO);

            assertEquals(2L, count);
        }

        @Test
        @DisplayName("삭제된 메시지는 카운트에서 제외")
        void deletedMessageExcluded() {
            saveMessage("msg1");
            Message m2 = saveMessage("msg2");
            m2.softDelete();
            messageRepository.save(m2);

            Long count = messageRepository.countUnreadMessages(chatRoom.getChatRoomId(), null, LONG_AGO);

            assertEquals(1L, count);
        }
    }

    @Nested
    @DisplayName("findNewMessages")
    class FindNewMessagesTest {

        @Test
        @DisplayName("since가 null이면 삭제되지 않은 전체 메시지 반환")
        void nullSince() {
            Message m1 = saveMessage("msg1");
            Message m2 = saveMessage("msg2");

            List<Message> result = messageRepository.findNewMessages(chatRoom.getChatRoomId(), null, LONG_AGO);

            List<Long> ids = result.stream().map(Message::getMessageId).toList();
            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> assertTrue(ids.containsAll(List.of(m1.getMessageId(), m2.getMessageId())))
            );
        }

        @Test
        @DisplayName("since보다 sendAt이 이전인 메시지는 반환하지 않음")
        void sinceInFuture() {
            saveMessage("msg1");
            saveMessage("msg2");

            List<Message> result = messageRepository.findNewMessages(
                    chatRoom.getChatRoomId(), LocalDateTime.now().plusMinutes(1), LONG_AGO);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("삭제된 메시지는 반환하지 않음")
        void deletedExcluded() {
            Message m1 = saveMessage("msg1");
            Message m2 = saveMessage("msg2");
            m2.softDelete();
            messageRepository.save(m2);

            List<Message> result = messageRepository.findNewMessages(chatRoom.getChatRoomId(), null, LONG_AGO);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(m1.getMessageId(), result.get(0).getMessageId())
            );
        }
    }

    @Nested
    @DisplayName("findUpdatedMessages")
    class FindUpdatedMessagesTest {

        @Test
        @DisplayName("sendAt <= since이고 updatedAt > since인 메시지 반환")
        void updatedAfterSince() {
            Message message = saveMessage("original");
            setSendAtInPast(message.getMessageId(), LocalDateTime.now().minusHours(2));

            LocalDateTime since = LocalDateTime.now().minusHours(1);

            Message saved = messageRepository.findById(message.getMessageId()).get();
            saved.update("edited"); // updatedAt = now() > since
            messageRepository.save(saved);

            List<Message> result = messageRepository.findUpdatedMessages(chatRoom.getChatRoomId(), since, LONG_AGO);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals("edited", result.get(0).getContent())
            );
        }

        @Test
        @DisplayName("수정되지 않은 메시지(updatedAt IS NULL)는 반환하지 않음")
        void notUpdatedExcluded() {
            Message message = saveMessage("original");
            setSendAtInPast(message.getMessageId(), LocalDateTime.now().minusHours(2));

            LocalDateTime since = LocalDateTime.now().minusHours(1);

            List<Message> result = messageRepository.findUpdatedMessages(chatRoom.getChatRoomId(), since, LONG_AGO);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("삭제된 메시지는 반환하지 않음")
        void deletedExcluded() {
            Message message = saveMessage("original");
            setSendAtInPast(message.getMessageId(), LocalDateTime.now().minusHours(2));

            LocalDateTime since = LocalDateTime.now().minusHours(1);

            Message saved = messageRepository.findById(message.getMessageId()).get();
            saved.update("edited");
            saved.softDelete();
            messageRepository.save(saved);

            List<Message> result = messageRepository.findUpdatedMessages(chatRoom.getChatRoomId(), since, LONG_AGO);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("sendAt이 since보다 나중인 메시지는 반환하지 않음")
        void sendAtAfterSinceExcluded() {
            Message message = saveMessage("original"); // sendAt = now()
            message.update("edited");
            messageRepository.save(message);

            // since = 1시간 전 → sendAt(now) > since 이므로 조건 미충족
            LocalDateTime since = LocalDateTime.now().minusHours(1);

            List<Message> result = messageRepository.findUpdatedMessages(chatRoom.getChatRoomId(), since, LONG_AGO);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findDeletedMessageIds")
    class FindDeletedMessageIdsTest {

        @Test
        @DisplayName("sendAt <= since이고 deletedAt > since인 메시지 ID 반환")
        void deletedAfterSince() {
            Message message = saveMessage("msg");
            setSendAtInPast(message.getMessageId(), LocalDateTime.now().minusHours(2));

            LocalDateTime since = LocalDateTime.now().minusHours(1);

            Message saved = messageRepository.findById(message.getMessageId()).get();
            saved.softDelete(); // deletedAt = now() > since
            messageRepository.save(saved);

            List<Long> result = messageRepository.findDeletedMessageIds(chatRoom.getChatRoomId(), since, LONG_AGO);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(message.getMessageId(), result.get(0))
            );
        }

        @Test
        @DisplayName("삭제되지 않은 메시지는 반환하지 않음")
        void notDeletedExcluded() {
            Message message = saveMessage("msg");
            setSendAtInPast(message.getMessageId(), LocalDateTime.now().minusHours(2));

            LocalDateTime since = LocalDateTime.now().minusHours(1);

            List<Long> result = messageRepository.findDeletedMessageIds(chatRoom.getChatRoomId(), since, LONG_AGO);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("sendAt이 since보다 나중인 메시지는 반환하지 않음")
        void sendAtAfterSinceExcluded() {
            Message message = saveMessage("msg"); // sendAt = now()
            message.softDelete();
            messageRepository.save(message);

            // since = 1시간 전 → sendAt(now) > since 이므로 조건 미충족
            LocalDateTime since = LocalDateTime.now().minusHours(1);

            List<Long> result = messageRepository.findDeletedMessageIds(chatRoom.getChatRoomId(), since, LONG_AGO);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("since 이전에 삭제된 메시지는 반환하지 않음")
        void deletedBeforeSinceExcluded() {
            Message message = saveMessage("msg");
            setSendAtInPast(message.getMessageId(), LocalDateTime.now().minusHours(3));

            // since보다 먼저 삭제
            Message saved = messageRepository.findById(message.getMessageId()).get();
            saved.softDelete();
            messageRepository.save(saved);

            // deletedAt(now) > since(now + 1시간) 가 되도록 since를 미래로 설정
            LocalDateTime since = LocalDateTime.now().plusHours(1);

            List<Long> result = messageRepository.findDeletedMessageIds(chatRoom.getChatRoomId(), since, LONG_AGO);

            assertTrue(result.isEmpty());
        }
    }
}
