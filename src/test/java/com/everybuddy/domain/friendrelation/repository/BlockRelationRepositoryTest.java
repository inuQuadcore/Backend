package com.everybuddy.domain.friendrelation.repository;

import com.everybuddy.domain.friendrelation.entity.BlockRelation;
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
@DisplayName("BlockRelationRepository 슬라이스 테스트")
class BlockRelationRepositoryTest {

    @Autowired private BlockRelationRepository blockRelationRepository;
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
    @DisplayName("existsBlockRelationBetween")
    class ExistsBlockRelationBetweenTest {

        @Test
        @DisplayName("A→B 차단, (A,B) 조회 - true (OR 좌측 브랜치)")
        void aBlocksB() {
            blockRelationRepository.save(BlockRelation.of(userA, userB));

            assertTrue(blockRelationRepository.existsBlockRelationBetween(
                    userA.getUserId(), userB.getUserId()));
        }

        @Test
        @DisplayName("B→A 차단, (A,B) 조회 - true (OR 우측 브랜치)")
        void bBlocksA() {
            blockRelationRepository.save(BlockRelation.of(userB, userA));

            assertTrue(blockRelationRepository.existsBlockRelationBetween(
                    userA.getUserId(), userB.getUserId()));
        }

        @Test
        @DisplayName("관계 없음 - false")
        void noRelation() {
            assertFalse(blockRelationRepository.existsBlockRelationBetween(
                    userA.getUserId(), userB.getUserId()));
        }
    }

    @Nested
    @DisplayName("findBlockRelatedUserIds")
    class FindBlockRelatedUserIdsTest {

        @Test
        @DisplayName("차단한 쪽·당한 쪽 모두 반환")
        void bothDirections() {
            blockRelationRepository.save(BlockRelation.of(userA, userB)); // A가 B를 차단
            blockRelationRepository.save(BlockRelation.of(userC, userA)); // C가 A를 차단

            List<Long> result = blockRelationRepository.findBlockRelatedUserIds(userA.getUserId());

            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> assertTrue(result.containsAll(List.of(userB.getUserId(), userC.getUserId())))
            );
        }
    }
}
