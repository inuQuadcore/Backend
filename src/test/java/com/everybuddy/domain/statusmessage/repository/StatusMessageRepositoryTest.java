package com.everybuddy.domain.statusmessage.repository;

import com.everybuddy.domain.friendrelation.entity.FriendRelation;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.statusmessage.entity.StatusMessage;
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
import org.springframework.data.domain.PageRequest;
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
@DisplayName("StatusMessageRepository 슬라이스 테스트")
class StatusMessageRepositoryTest {

    @Autowired private StatusMessageRepository statusMessageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendRelationRepository friendRelationRepository;
    @Autowired private TestEntityManager em;

    private User me;
    private User friend;
    private User stranger;

    @BeforeEach
    void setUp() {
        me = userRepository.save(User.createForTest(null, "me", "Me", "pw",
                Country.KOREA, Gender.MALE, LocalDate.of(1995, 1, 1)));
        friend = userRepository.save(User.createForTest(null, "friend", "Friend", "pw",
                Country.KOREA, Gender.FEMALE, LocalDate.of(1996, 2, 2)));
        stranger = userRepository.save(User.createForTest(null, "stranger", "Stranger", "pw",
                Country.KOREA, Gender.MALE, LocalDate.of(1997, 3, 3)));

        friendRelationRepository.save(FriendRelation.of(me, friend));
    }

    private StatusMessage saveStatusMessage(User user, String content) {
        return statusMessageRepository.save(StatusMessage.of(user, content));
    }

    /**
     * updatedAt은 @LastModifiedDate로 자동 세팅되므로,
     * 커서 테스트에서 특정 시점이 필요한 경우 저장 후 native SQL로 직접 세팅한다.
     */
    private void setUpdatedAt(Long statusMessageId, LocalDateTime updatedAt) {
        em.getEntityManager()
                .createNativeQuery("UPDATE status_message SET updated_at = :updatedAt WHERE status_message_id = :id")
                .setParameter("updatedAt", updatedAt)
                .setParameter("id", statusMessageId)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findFriendStatusMessages")
    class FindFriendStatusMessagesTest {

        @Test
        @DisplayName("친구의 상태메시지만 반환")
        void onlyFriendStatusMessages() {
            StatusMessage friendSm = saveStatusMessage(friend, "friend msg");
            saveStatusMessage(stranger, "stranger msg");

            List<StatusMessage> result = statusMessageRepository.findFriendStatusMessages(
                    me.getUserId(), PageRequest.of(0, 10));

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(friendSm.getStatusMessageId(), result.get(0).getStatusMessageId())
            );
        }

        @Test
        @DisplayName("친구 관계가 없으면 빈 리스트 반환")
        void noFriends() {
            saveStatusMessage(stranger, "stranger msg");

            List<StatusMessage> result = statusMessageRepository.findFriendStatusMessages(
                    me.getUserId(), PageRequest.of(0, 10));

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("삭제된 상태메시지는 반환하지 않음")
        void deletedStatusMessageExcluded() {
            StatusMessage sm = saveStatusMessage(friend, "msg");
            sm.softDelete();
            statusMessageRepository.save(sm);

            List<StatusMessage> result = statusMessageRepository.findFriendStatusMessages(
                    me.getUserId(), PageRequest.of(0, 10));

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("탈퇴한 유저의 상태메시지는 반환하지 않음")
        void deletedUserExcluded() {
            saveStatusMessage(friend, "msg");
            friend.softDelete();
            userRepository.save(friend);

            List<StatusMessage> result = statusMessageRepository.findFriendStatusMessages(
                    me.getUserId(), PageRequest.of(0, 10));

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findFriendStatusMessagesAfterCursor")
    class FindFriendStatusMessagesAfterCursorTest {

        @Test
        @DisplayName("cursorUpdatedAt보다 이전에 수정된 메시지만 반환")
        void beforeCursorUpdatedAt() {
            StatusMessage older = saveStatusMessage(friend, "older");
            StatusMessage newer = saveStatusMessage(friend, "newer");

            setUpdatedAt(older.getStatusMessageId(), LocalDateTime.now().minusHours(2));
            setUpdatedAt(newer.getStatusMessageId(), LocalDateTime.now().minusHours(1));

            // 서비스와 동일하게 DB에서 읽은 updatedAt을 커서 파라미터로 사용
            StatusMessage loadedNewer = statusMessageRepository.findById(newer.getStatusMessageId()).get();

            List<StatusMessage> result = statusMessageRepository.findFriendStatusMessagesAfterCursor(
                    me.getUserId(), loadedNewer.getUpdatedAt(), newer.getStatusMessageId(), PageRequest.of(0, 10));

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(older.getStatusMessageId(), result.get(0).getStatusMessageId())
            );
        }

        @Test
        @DisplayName("updatedAt이 같으면 statusMessageId가 작은 메시지만 반환")
        void sameUpdatedAtUsesIdTieBreaker() {
            StatusMessage sm1 = saveStatusMessage(friend, "first");
            StatusMessage sm2 = saveStatusMessage(friend, "second");

            LocalDateTime sameTime = LocalDateTime.now().minusHours(1);
            setUpdatedAt(sm1.getStatusMessageId(), sameTime);
            setUpdatedAt(sm2.getStatusMessageId(), sameTime);

            // 서비스와 동일하게 DB에서 읽은 updatedAt을 커서 파라미터로 사용
            StatusMessage loadedSm2 = statusMessageRepository.findById(sm2.getStatusMessageId()).get();

            // sm1.id < sm2.id, cursor = sm2 → sm1만 반환
            List<StatusMessage> result = statusMessageRepository.findFriendStatusMessagesAfterCursor(
                    me.getUserId(), loadedSm2.getUpdatedAt(), sm2.getStatusMessageId(), PageRequest.of(0, 10));

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(sm1.getStatusMessageId(), result.get(0).getStatusMessageId())
            );
        }
    }
}
