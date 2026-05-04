package com.everybuddy.domain.fcmtoken.service;

import com.everybuddy.domain.fcmtoken.dto.FcmTokenRegisterRequest;
import com.everybuddy.domain.fcmtoken.entity.FcmToken;
import com.everybuddy.domain.fcmtoken.repository.FcmTokenRepository;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenService 단위 테스트")
class FcmTokenServiceTest {

    @Mock private FcmTokenRepository fcmTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createForTest(1L, "user1", "홍길동", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("기존 토큰 없으면 신규 저장")
        void savesNewWhenNoExisting() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(fcmTokenRepository.findByUser(user)).thenReturn(Optional.empty());

            FcmTokenRegisterRequest request = FcmTokenRegisterRequest.ofForTest("new-token");
            fcmTokenService.register(1L, request);

            ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);
            verify(fcmTokenRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals(1L, captor.getValue().getUser().getUserId()),
                    () -> assertEquals("new-token", captor.getValue().getToken())
            );
        }

        @Test
        @DisplayName("기존 토큰 있으면 새 값으로 갱신 (save 호출 X)")
        void updatesExistingToken() {
            FcmToken existing = FcmToken.createForTest(10L, user, "old-token");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(fcmTokenRepository.findByUser(user)).thenReturn(Optional.of(existing));

            FcmTokenRegisterRequest request = FcmTokenRegisterRequest.ofForTest("new-token");
            fcmTokenService.register(1L, request);

            assertEquals("new-token", existing.getToken());
            verify(fcmTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("유저가 없으면 USER_NOT_FOUND")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            FcmTokenRegisterRequest request = FcmTokenRegisterRequest.ofForTest("any-token");
            CustomException exception = assertThrows(CustomException.class,
                    () -> fcmTokenService.register(1L, request));

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(fcmTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("유저의 토큰을 삭제")
        void deletesUserToken() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            fcmTokenService.delete(1L);

            verify(fcmTokenRepository).deleteByUser(user);
        }

        @Test
        @DisplayName("유저가 없으면 USER_NOT_FOUND")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> fcmTokenService.delete(1L));

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(fcmTokenRepository, never()).deleteByUser(any());
        }
    }
}
