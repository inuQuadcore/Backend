package com.everybuddy.domain.friendrelation.service;

import com.everybuddy.domain.friendrelation.entity.FriendRelation;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
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

import java.time.LocalDate;
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

    @InjectMocks
    private FriendRelationService friendRelationService;

    private User userA;
    private User userB;
    private User deletedUser;

    @BeforeEach
    void setUp() {
        userA = User.createForTest(1L, "userA", "유저A", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        userB = User.createForTest(2L, "userB", "유저B", "password",
                Country.KOREA, Gender.FEMALE, LocalDate.of(1992, 1, 1));
        deletedUser = User.createForTest(3L, "deleted", "탈퇴유저", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1995, 1, 1));
        deletedUser.softDelete();
    }

    @Nested
    @DisplayName("1. addFriend() - 성공")
    class AddFriendSuccessCases {

        @Test
        @DisplayName("TC-1-1. 친구 추가 성공")
        void success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(blockRelationRepository.existsBlockRelationBetween(1L, 2L)).thenReturn(false);
            when(friendRelationRepository.existsFriendRelationBetween(1L, 2L)).thenReturn(false);

            friendRelationService.addFriend(1L, 2L);

            ArgumentCaptor<FriendRelation> captor = ArgumentCaptor.forClass(FriendRelation.class);
            verify(friendRelationRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals(1L, captor.getValue().getFromUser().getUserId()),
                    () -> assertEquals(2L, captor.getValue().getToUser().getUserId())
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
            when(userRepository.findById(3L)).thenReturn(Optional.of(deletedUser));

            CustomException ex = assertThrows(CustomException.class,
                    () -> friendRelationService.addFriend(1L, 3L));

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
}
