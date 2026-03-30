package com.everybuddy.domain.discover.service;

import com.everybuddy.domain.discover.dto.FilterDiscoverRequest;
import com.everybuddy.domain.discover.dto.FilterDiscoverResponse;
import com.everybuddy.domain.discover.dto.RandomDiscoverResponse;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserPresenceRepository;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.domain.user.service.UserProfileLoader;
import com.everybuddy.global.s3.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscoverService 단위 테스트")
class DiscoverServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserPresenceRepository userPresenceRepository;
    @Mock private FriendRelationRepository friendRelationRepository;
    @Mock private BlockRelationRepository blockRelationRepository;
    @Mock private UserProfileLoader userProfileLoader;
    @Mock private StorageService storageService;

    @InjectMocks
    private DiscoverService discoverService;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        userA = User.createForTest(1L, "userA", "유저A", "pw", Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        userB = User.createForTest(2L, "userB", "유저B", "pw", Country.KOREA, Gender.FEMALE, LocalDate.of(1992, 1, 1));
        userC = User.createForTest(3L, "userC", "유저C", "pw", Country.USA, Gender.MALE, LocalDate.of(1995, 1, 1));
    }

    private void stubResponseBuilding() {
        when(userProfileLoader.loadLanguagesByUserId(any())).thenReturn(Map.of());
        when(userProfileLoader.loadTagsByUserId(any())).thenReturn(Map.of());
        when(userPresenceRepository.findAllByUserIdIn(any())).thenReturn(List.of());
    }

    @Nested
    @DisplayName("1. getRandomUsers() - excludeIds 구성")
    class GetRandomUsersExcludeIds {

        @ParameterizedTest(name = "friendIds={0}, blockIds={1} → excludeIds={2}")
        @MethodSource("excludeIdsProvider")
        @DisplayName("TC-1. 본인+친구+차단 관계가 excludeIds에 포함된다")
        void excludeIdsBuiltCorrectly(List<Long> friendIds, List<Long> blockIds, List<Long> expectedExcludeIds) {
            when(friendRelationRepository.findFriendIds(1L)).thenReturn(friendIds);
            when(blockRelationRepository.findBlockRelatedUserIds(1L)).thenReturn(blockIds);
            when(userRepository.findRandomUsers(any())).thenReturn(List.of());
            stubResponseBuilding();

            discoverService.getRandomUsers(1L);

            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            verify(userRepository).findRandomUsers(captor.capture());
            assertAll(
                    () -> assertEquals(expectedExcludeIds.size(), captor.getValue().size()),
                    () -> assertTrue(captor.getValue().containsAll(expectedExcludeIds))
            );
        }

        static Stream<Arguments> excludeIdsProvider() {
            return Stream.of(
                    Arguments.of(List.of(), List.of(), List.of(1L)),
                    Arguments.of(List.of(2L), List.of(3L), List.of(1L, 2L, 3L))
            );
        }
    }

    @Nested
    @DisplayName("2. getRandomUsers() - 응답")
    class GetRandomUsersResponse {

        @Test
        @DisplayName("TC-2. 조회된 유저 목록이 응답에 포함된다")
        void returnsDiscoveredUsers() {
            when(friendRelationRepository.findFriendIds(1L)).thenReturn(List.of());
            when(blockRelationRepository.findBlockRelatedUserIds(1L)).thenReturn(List.of());
            when(userRepository.findRandomUsers(List.of(1L))).thenReturn(List.of(userB, userC));
            stubResponseBuilding();

            RandomDiscoverResponse response = discoverService.getRandomUsers(1L);

            assertAll(
                    () -> assertEquals(2, response.getUsers().size()),
                    () -> assertEquals(2L, response.getUsers().get(0).getUserId()),
                    () -> assertEquals(3L, response.getUsers().get(1).getUserId())
            );
        }
    }

    @Nested
    @DisplayName("3. getFilteredUsers() - 페이징")
    class GetFilteredUsersPagination {

        private static final FilterDiscoverRequest REQUEST_SIZE_2 =
                new FilterDiscoverRequest(null, null, null, null, null, null, false, false, null, 2);

        @Test
        @DisplayName("TC-3. size+1개 반환 → hasNext=true, nextCursor=마지막 userId")
        void hasNextTrue() {
            when(friendRelationRepository.findFriendIds(1L)).thenReturn(List.of());
            when(blockRelationRepository.findBlockRelatedUserIds(1L)).thenReturn(List.of());
            when(userRepository.findFilteredUsers(any(), any(), any(), any(), any(),
                    anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), anyBoolean(), any(), any(), any()))
                    .thenReturn(List.of(userA, userB, userC));
            stubResponseBuilding();

            FilterDiscoverResponse response = discoverService.getFilteredUsers(1L, REQUEST_SIZE_2);

            assertAll(
                    () -> assertTrue(response.isHasNext()),
                    () -> assertEquals(2L, response.getNextCursor()),
                    () -> assertEquals(2, response.getUsers().size())
            );
        }

        @ParameterizedTest(name = "반환={0}개 → nextCursorNull={1}")
        @MethodSource("hasNextFalseProvider")
        @DisplayName("TC-4~5. size 이하 반환 → hasNext=false")
        void hasNextFalse(List<User> returnedUsers, boolean expectNullCursor) {
            when(friendRelationRepository.findFriendIds(1L)).thenReturn(List.of());
            when(blockRelationRepository.findBlockRelatedUserIds(1L)).thenReturn(List.of());
            when(userRepository.findFilteredUsers(any(), any(), any(), any(), any(),
                    anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), anyBoolean(), any(), any(), any()))
                    .thenReturn(returnedUsers);
            stubResponseBuilding();

            FilterDiscoverResponse response = discoverService.getFilteredUsers(1L, REQUEST_SIZE_2);

            assertFalse(response.isHasNext());
            if (expectNullCursor) {
                assertNull(response.getNextCursor());
            } else {
                assertNotNull(response.getNextCursor());
            }
        }

        static Stream<Arguments> hasNextFalseProvider() {
            User u1 = User.createForTest(2L, "b", "유저B", "pw", Country.KOREA, Gender.FEMALE, LocalDate.of(1992, 1, 1));
            User u2 = User.createForTest(3L, "c", "유저C", "pw", Country.USA, Gender.MALE, LocalDate.of(1995, 1, 1));
            return Stream.of(
                    Arguments.of(List.of(u1, u2), false),  // size=2, returned=2 → nextCursor 있음
                    Arguments.of(List.of(), true)           // returned=0 → nextCursor=null
            );
        }
    }
}
