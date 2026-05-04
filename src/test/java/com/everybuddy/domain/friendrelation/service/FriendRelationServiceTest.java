package com.everybuddy.domain.friendrelation.service;

import com.everybuddy.domain.friendrelation.dto.FriendListResponse;
import com.everybuddy.domain.friendrelation.entity.FriendRelation;
import com.everybuddy.domain.friendrelation.event.FriendAddedEvent;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Tag;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserTag;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.domain.user.service.UserProfileLoader;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.s3.service.StorageService;
import com.everybuddy.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendRelationService 단위 테스트")
class FriendRelationServiceTest {

    @Mock private FriendRelationRepository friendRelationRepository;
    @Mock private BlockRelationRepository blockRelationRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserProfileLoader userProfileLoader;
    @Mock private StorageService storageService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendRelationService friendRelationService;

    private User userA;
    private User userB;
    private User userC;
    private User deletedUser;

    @BeforeEach
    void setUp() {
        userA = User.createForTest(1L, "userA", "유저A", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        userB = User.createForTest(2L, "userB", "유저B", "password",
                Country.KOREA, Gender.FEMALE, LocalDate.of(1992, 1, 1));
        userC = User.createForTest(3L, "userC", "유저C", "password",
                Country.USA, Gender.MALE, LocalDate.of(1995, 1, 1));
        deletedUser = User.createForTest(4L, "deleted", "탈퇴유저", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1995, 1, 1));
        deletedUser.softDelete();
    }

    @Nested
    @DisplayName("1. addFriend() - 성공")
    class AddFriendSuccessCases {

        @Test
        @DisplayName("TC-1-1. 친구 추가 성공 + FriendAddedEvent 발행")
        void success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(blockRelationRepository.existsBlockRelationBetween(1L, 2L)).thenReturn(false);
            when(friendRelationRepository.existsFriendRelationBetween(1L, 2L)).thenReturn(false);

            friendRelationService.addFriend(1L, 2L);

            ArgumentCaptor<FriendRelation> relationCaptor = ArgumentCaptor.forClass(FriendRelation.class);
            verify(friendRelationRepository).save(relationCaptor.capture());
            assertAll(
                    () -> assertEquals(1L, relationCaptor.getValue().getFromUser().getUserId()),
                    () -> assertEquals(2L, relationCaptor.getValue().getToUser().getUserId())
            );

            ArgumentCaptor<FriendAddedEvent> eventCaptor = ArgumentCaptor.forClass(FriendAddedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertAll(
                    () -> assertEquals(1L, eventCaptor.getValue().getFromUser().getUserId()),
                    () -> assertEquals(2L, eventCaptor.getValue().getToUser().getUserId())
            );
        }
    }

    @Nested
    @DisplayName("2. addFriend() - 실패")
    class AddFriendFailCases {

        @Test
        @DisplayName("TC-2-1. 자기 자신을 친구 추가 → CANNOT_ADD_SELF")
        void failSelfAdd() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> friendRelationService.addFriend(1L, 1L));

            assertEquals(ErrorCode.CANNOT_ADD_SELF, ex.getErrorCode());
            verify(friendRelationRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-2. toUser 없음 → USER_NOT_FOUND")
        void failToUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> friendRelationService.addFriend(1L, 2L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(friendRelationRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-3. toUser 탈퇴 → USER_DELETED")
        void failToUserDeleted() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(4L)).thenReturn(Optional.of(deletedUser));

            CustomException ex = assertThrows(CustomException.class,
                    () -> friendRelationService.addFriend(1L, 4L));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(friendRelationRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-4. 차단 관계 있음 → USER_NOT_FOUND")
        void failBlocked() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(blockRelationRepository.existsBlockRelationBetween(1L, 2L)).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> friendRelationService.addFriend(1L, 2L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(friendRelationRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-5. 이미 친구 → ALREADY_FRIEND")
        void failAlreadyFriend() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(blockRelationRepository.existsBlockRelationBetween(1L, 2L)).thenReturn(false);
            when(friendRelationRepository.existsFriendRelationBetween(1L, 2L)).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> friendRelationService.addFriend(1L, 2L));

            assertEquals(ErrorCode.ALREADY_FRIEND, ex.getErrorCode());
            verify(friendRelationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("3. getFriends() - 성공")
    class GetFriendsSuccessCases {

        @Test
        @DisplayName("TC-3-1. 친구가 있을 때 → 언어/태그 포함 정상 반환")
        void successWithFriends() {
            FriendRelation relation = FriendRelation.of(userA, userB);

            UserLanguage language = UserLanguage.of(userB, Language.ENGLISH, 3);
            UserTag tag = UserTag.of(userB, Tag.SPORTS);

            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(friendRelationRepository.findAllFriends(1L)).thenReturn(List.of(relation));
            when(userProfileLoader.loadLanguagesByUserId(List.of(2L))).thenReturn(Map.of(2L, List.of(language)));
            when(userProfileLoader.loadTagsByUserId(List.of(2L))).thenReturn(Map.of(2L, List.of(tag)));

            FriendListResponse response = friendRelationService.getFriends(1L);

            assertAll(
                    () -> assertEquals(1, response.getFriends().size()),
                    () -> assertEquals(2L, response.getFriends().getFirst().getUserId()),
                    () -> assertEquals("유저B", response.getFriends().getFirst().getName()),
                    () -> assertEquals(1, response.getFriends().getFirst().getLanguages().size()),
                    () -> assertEquals("ENGLISH", response.getFriends().getFirst().getLanguages().getFirst().getLanguage()),
                    () -> assertEquals(3, response.getFriends().getFirst().getLanguages().getFirst().getLevel()),
                    () -> assertEquals(1, response.getFriends().getFirst().getTags().size()),
                    () -> assertEquals("SPORTS", response.getFriends().getFirst().getTags().getFirst().getTag())
            );
        }

        @Test
        @DisplayName("TC-3-2. 친구가 없을 때 → 빈 리스트 반환")
        void successWithNoFriends() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(friendRelationRepository.findAllFriends(1L)).thenReturn(List.of());

            FriendListResponse response = friendRelationService.getFriends(1L);

            assertTrue(response.getFriends().isEmpty());
        }

        @Test
        @DisplayName("TC-3-3. 여러 친구 → 전체 반환")
        void successWithMultipleFriends() {
            FriendRelation relation1 = FriendRelation.of(userA, userB);
            FriendRelation relation2 = FriendRelation.of(userA, userC);

            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(friendRelationRepository.findAllFriends(1L)).thenReturn(List.of(relation1, relation2));

            FriendListResponse response = friendRelationService.getFriends(1L);

            assertEquals(2, response.getFriends().size());
        }

        @Test
        @DisplayName("TC-3-4. 언어/태그 없는 친구 → 해당 필드 빈 리스트 반환")
        void successWithNoLanguagesAndTags() {
            FriendRelation relation = FriendRelation.of(userA, userB);

            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(friendRelationRepository.findAllFriends(1L)).thenReturn(List.of(relation));
            when(userProfileLoader.loadLanguagesByUserId(List.of(2L))).thenReturn(Map.of());
            when(userProfileLoader.loadTagsByUserId(List.of(2L))).thenReturn(Map.of());

            FriendListResponse response = friendRelationService.getFriends(1L);

            assertAll(
                    () -> assertTrue(response.getFriends().getFirst().getLanguages().isEmpty()),
                    () -> assertTrue(response.getFriends().getFirst().getTags().isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("4. getFriends() - 실패")
    class GetFriendsFailCases {

        @Test
        @DisplayName("TC-4-1. 존재하지 않는 userId → USER_NOT_FOUND")
        void failUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> friendRelationService.getFriends(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }
    }
}
