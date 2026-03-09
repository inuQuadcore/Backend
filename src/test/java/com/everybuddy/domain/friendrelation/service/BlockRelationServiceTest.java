package com.everybuddy.domain.friendrelation.service;

import com.everybuddy.domain.friendrelation.entity.BlockRelation;
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
@DisplayName("BlockRelationService 단위 테스트")
class BlockRelationServiceTest {

    @Mock private BlockRelationRepository blockRelationRepository;
    @Mock private FriendRelationRepository friendRelationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private BlockRelationService blockRelationService;

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
    @DisplayName("1. block() - 성공")
    class BlockSuccessCases {

        @Test
        @DisplayName("TC-1-1. 친구 관계 없는 사람 차단 성공")
        void successWithoutFriendRelation() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(blockRelationRepository.existsBlock(1L, 2L)).thenReturn(false);

            blockRelationService.block(1L, 2L);

            verify(friendRelationRepository).deleteFriendRelationBetween(1L, 2L);
            ArgumentCaptor<BlockRelation> captor = ArgumentCaptor.forClass(BlockRelation.class);
            verify(blockRelationRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals(1L, captor.getValue().getBlockerUser().getUserId()),
                    () -> assertEquals(2L, captor.getValue().getBlockedUser().getUserId())
            );
        }

        @Test
        @DisplayName("TC-1-2. 친구 관계 있는 사람 차단 시 친구 관계 삭제")
        void successWithFriendRelation() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(blockRelationRepository.existsBlock(1L, 2L)).thenReturn(false);

            blockRelationService.block(1L, 2L);

            verify(friendRelationRepository).deleteFriendRelationBetween(1L, 2L);
            verify(blockRelationRepository).save(any(BlockRelation.class));
        }
    }

    @Nested
    @DisplayName("2. block() - 실패")
    class BlockFailCases {

        @Test
        @DisplayName("TC-2-1. 자기 자신 차단 → CANNOT_BLOCK_SELF")
        void failSelfBlock() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> blockRelationService.block(1L, 1L));

            assertEquals(ErrorCode.CANNOT_BLOCK_SELF, ex.getErrorCode());
            verify(blockRelationRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-2. 이미 차단한 사용자 → ALREADY_BLOCKED")
        void failAlreadyBlocked() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(blockRelationRepository.existsBlock(1L, 2L)).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> blockRelationService.block(1L, 2L));

            assertEquals(ErrorCode.ALREADY_BLOCKED, ex.getErrorCode());
            verify(blockRelationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("3. unblock() - 성공")
    class UnblockSuccessCases {

        @Test
        @DisplayName("TC-3-1. 차단 해제 성공")
        void success() {
            when(blockRelationRepository.existsBlock(1L, 2L)).thenReturn(true);

            blockRelationService.unblock(1L, 2L);

            verify(blockRelationRepository).deleteBlock(1L, 2L);
        }
    }

    @Nested
    @DisplayName("4. unblock() - 실패")
    class UnblockFailCases {

        @Test
        @DisplayName("TC-4-1. 차단 관계 없음 → BLOCK_NOT_FOUND")
        void failBlockNotFound() {
            when(blockRelationRepository.existsBlock(1L, 2L)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> blockRelationService.unblock(1L, 2L));

            assertEquals(ErrorCode.BLOCK_NOT_FOUND, ex.getErrorCode());
            verify(blockRelationRepository, never()).deleteBlock(any(), any());
        }
    }
}
