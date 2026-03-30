package com.everybuddy.domain.user.repository;

import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Tag;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserPresence;
import com.everybuddy.domain.user.entity.UserTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@DisplayName("UserRepository 슬라이스 테스트")
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private UserPresenceRepository userPresenceRepository;
    @Autowired private UserLanguageRepository userLanguageRepository;
    @Autowired private UserTagRepository userTagRepository;

    private User requester; // 요청자 역할, 항상 excludeIds에 포함 (buildExcludeIds 동작 재현)
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        requester = userRepository.save(User.createForTest(null, "loginReq", "Requester", "pw",
                Country.USA, Gender.OTHER, LocalDate.of(1988, 7, 20)));
        userA = userRepository.save(User.createForTest(null, "loginA", "A", "pw",
                Country.KOREA, Gender.MALE, LocalDate.of(2000, 1, 1)));
        userB = userRepository.save(User.createForTest(null, "loginB", "B", "pw",
                Country.JAPAN, Gender.FEMALE, LocalDate.of(1990, 6, 15)));
    }

    private List<User> findFiltered(
            List<Long> excludeIds, Gender gender, Country country,
            LocalDate maxBirthday, LocalDate minBirthday,
            List<Language> languages, List<Tag> tags,
            boolean isOnline, boolean recentlyActive, LocalDateTime cutoff,
            Long lastUserId) {
        return userRepository.findFilteredUsers(
                excludeIds, gender, country, maxBirthday, minBirthday,
                !languages.isEmpty(), languages,
                !tags.isEmpty(), tags,
                isOnline, recentlyActive, cutoff, lastUserId,
                PageRequest.of(0, 100)
        );
    }

    @Nested
    @DisplayName("findFilteredUsers")
    class FindFilteredUsersTest {

        @Test
        @DisplayName("필터 없음 - deletedAt 유저 제외, 나머지 반환")
        void noFilter() {
            User deletedUser = User.createForTest(null, "loginC", "C", "pw",
                    Country.KOREA, Gender.MALE, LocalDate.of(1985, 3, 10));
            deletedUser.softDelete();
            userRepository.save(deletedUser);

            List<User> result = findFiltered(List.of(requester.getUserId()), null, null, null, null,
                    List.of(), List.of(), false, false, null, null);

            List<Long> resultIds = result.stream().map(User::getUserId).toList();
            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> assertTrue(resultIds.containsAll(List.of(userA.getUserId(), userB.getUserId())))
            );
        }

        @ParameterizedTest
        @EnumSource(value = Gender.class, names = {"MALE", "FEMALE"})
        @DisplayName("gender 필터 - 해당 gender 유저만 반환")
        void genderFilter(Gender gender) {
            List<User> result = findFiltered(List.of(requester.getUserId()), gender, null, null, null,
                    List.of(), List.of(), false, false, null, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(gender, result.get(0).getGender())
            );
        }

        @ParameterizedTest
        @MethodSource("countryProvider")
        @DisplayName("country 필터 - 해당 country 유저만 반환")
        void countryFilter(Country country) {
            List<User> result = findFiltered(List.of(requester.getUserId()), null, country, null, null,
                    List.of(), List.of(), false, false, null, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(country, result.get(0).getCountry())
            );
        }

        static Stream<Country> countryProvider() {
            return Stream.of(Country.KOREA, Country.JAPAN);
        }

        @Test
        @DisplayName("maxBirthday - 경계값 이하인 유저만 반환")
        void maxBirthdayFilter() {
            List<User> result = findFiltered(List.of(requester.getUserId()), null, null, LocalDate.of(1995, 1, 1), null,
                    List.of(), List.of(), false, false, null, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userB.getUserId(), result.get(0).getUserId())
            );
        }

        @Test
        @DisplayName("minBirthday - 경계값 이상인 유저만 반환")
        void minBirthdayFilter() {
            List<User> result = findFiltered(List.of(requester.getUserId()), null, null, null, LocalDate.of(1995, 1, 1),
                    List.of(), List.of(), false, false, null, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userA.getUserId(), result.get(0).getUserId())
            );
        }

        @Test
        @DisplayName("language 필터 - 해당 언어 가진 유저만 반환")
        void languageFilter() {
            userLanguageRepository.save(UserLanguage.of(userA, Language.ENGLISH, 3));
            userLanguageRepository.save(UserLanguage.of(userB, Language.JAPANESE, 2));

            List<User> result = findFiltered(List.of(requester.getUserId()), null, null, null, null,
                    List.of(Language.ENGLISH), List.of(), false, false, null, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userA.getUserId(), result.get(0).getUserId())
            );
        }

        @Test
        @DisplayName("tag 필터 - 해당 태그 가진 유저만 반환")
        void tagFilter() {
            userTagRepository.save(UserTag.of(userA, Tag.SPORTS));
            userTagRepository.save(UserTag.of(userB, Tag.MUSIC));

            List<User> result = findFiltered(List.of(requester.getUserId()), null, null, null, null,
                    List.of(), List.of(Tag.SPORTS), false, false, null, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userA.getUserId(), result.get(0).getUserId())
            );
        }

        @Test
        @DisplayName("isOnline=true - is_online이 true인 유저만 반환")
        void isOnlineFilter() {
            userPresenceRepository.save(UserPresence.of(userA));
            userPresenceRepository.save(UserPresence.of(userB));
            userPresenceRepository.markOnline(userA.getUserId());

            List<User> result = findFiltered(List.of(requester.getUserId()), null, null, null, null,
                    List.of(), List.of(), true, false, null, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userA.getUserId(), result.get(0).getUserId())
            );
        }

        @Test
        @DisplayName("recentlyActive=true - cutoff 이후 접속한 유저만 반환")
        void recentlyActiveFilter() {
            userPresenceRepository.save(UserPresence.of(userA));
            userPresenceRepository.save(UserPresence.of(userB));
            userPresenceRepository.markOnline(userA.getUserId()); // lastSeenAt = now, userB는 null

            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(10);

            List<User> result = findFiltered(List.of(requester.getUserId()), null, null, null, null,
                    List.of(), List.of(), false, true, cutoff, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userA.getUserId(), result.get(0).getUserId())
            );
        }

        @Test
        @DisplayName("lastUserId 커서 - userId > lastUserId인 유저만 반환")
        void lastUserIdCursor() {
            List<User> result = findFiltered(List.of(requester.getUserId()), null, null, null, null,
                    List.of(), List.of(), false, false, null, userA.getUserId());

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userB.getUserId(), result.get(0).getUserId())
            );
        }

        @Test
        @DisplayName("excludeIds - 해당 유저 제외")
        void excludeIds() {
            List<User> result = findFiltered(List.of(requester.getUserId(), userA.getUserId()), null, null, null, null,
                    List.of(), List.of(), false, false, null, null);

            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(userB.getUserId(), result.get(0).getUserId())
            );
        }
    }

    @Nested
    @DisplayName("findRandomUsers")
    class FindRandomUsersTest {

        @Test
        @DisplayName("excludeIds - 해당 유저 결과에 없음")
        void excludeIds() {
            List<User> result = userRepository.findRandomUsers(List.of(requester.getUserId(), userA.getUserId()));

            List<Long> resultIds = result.stream().map(User::getUserId).toList();
            assertFalse(resultIds.contains(userA.getUserId()));
        }

        @Test
        @DisplayName("deletedAt - 탈퇴 유저 결과에 없음")
        void deletedAtExcluded() {
            User deletedUser = User.createForTest(null, "loginC", "C", "pw",
                    Country.KOREA, Gender.MALE, LocalDate.of(1985, 3, 10));
            deletedUser.softDelete();
            User saved = userRepository.save(deletedUser);

            List<User> result = userRepository.findRandomUsers(List.of(requester.getUserId()));

            List<Long> resultIds = result.stream().map(User::getUserId).toList();
            assertFalse(resultIds.contains(saved.getUserId()));
        }
    }
}
