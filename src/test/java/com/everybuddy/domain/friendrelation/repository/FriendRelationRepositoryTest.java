package com.everybuddy.domain.friendrelation.repository;

import com.everybuddy.domain.friendrelation.entity.FriendRelation;
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
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@DisplayName("FriendRelationRepository 슬라이스 테스트")
class FriendRelationRepositoryTest {

    @Autowired private FriendRelationRepository friendRelationRepository;
    @Autowired private UserRepository userRepository;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.createForTest(null, "loginA", "A", "pw",
                Country.KOREA, Gender.MALE, LocalDate.of(2000, 1, 1)));
        userB = userRepository.save(User.createForTest(null, "loginB", "B", "pw",
                Country.KOREA, Gender.MALE, LocalDate.of(2000, 1, 1)));
        userC = userRepository.save(User.createForTest(null, "loginC", "C", "pw",
                Country.KOREA, Gender.MALE, LocalDate.of(2000, 1, 1)));
    }

    @Nested
    @DisplayName("existsFriendRelationBetween")
    class ExistsFriendRelationBetweenTest {

        @Test
        @DisplayName("A→B 저장, (A,B) 조회 - true (OR 좌측 브랜치)")
        void aToB() {
            friendRelationRepository.save(FriendRelation.of(userA, userB));

            assertTrue(friendRelationRepository.existsFriendRelationBetween(
                    userA.getUserId(), userB.getUserId()));
        }

        @Test
        @DisplayName("B→A 저장, (A,B) 조회 - true (OR 우측 브랜치)")
        void bToA() {
            friendRelationRepository.save(FriendRelation.of(userB, userA));

            assertTrue(friendRelationRepository.existsFriendRelationBetween(
                    userA.getUserId(), userB.getUserId()));
        }

        @Test
        @DisplayName("관계 없음 - false")
        void noRelation() {
            assertFalse(friendRelationRepository.existsFriendRelationBetween(
                    userA.getUserId(), userB.getUserId()));
        }
    }

    @Nested
    @DisplayName("findFriendIds")
    class FindFriendIdsTest {

        @Test
        @DisplayName("fromUser 방향 관계 - 상대방 userId 반환")
        void fromUserDirection() {
            friendRelationRepository.save(FriendRelation.of(userA, userB));
            friendRelationRepository.save(FriendRelation.of(userA, userC));

            List<Long> result = friendRelationRepository.findFriendIds(userA.getUserId());

            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> assertTrue(result.containsAll(List.of(userB.getUserId(), userC.getUserId())))
            );
        }

        @Test
        @DisplayName("toUser 방향 관계 - 상대방 userId 반환")
        void toUserDirection() {
            friendRelationRepository.save(FriendRelation.of(userB, userA));

            List<Long> result = friendRelationRepository.findFriendIds(userA.getUserId());

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userB.getUserId(), result.get(0))
            );
        }
    }
}
