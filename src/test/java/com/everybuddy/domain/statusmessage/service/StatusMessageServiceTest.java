package com.everybuddy.domain.statusmessage.service;

import com.everybuddy.domain.statusmessage.dto.CreateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.dto.UpdateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import com.everybuddy.domain.statusmessage.repository.StatusMessageRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatusMessageService 단위 테스트")
class StatusMessageServiceTest {

    @Mock private StatusMessageRepository statusMessageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private StatusMessageService statusMessageService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createForTest(1L, "user1", "홍길동", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
    }

    @Nested
    @DisplayName("1. createStatusMessage() - 성공")
    class CreateStatusMessageSuccessCases {

        @Test
        @DisplayName("TC-1-1. 기존 메시지 없을 때 작성 성공")
        void success() {
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            CreateStatusMessageRequest request = CreateStatusMessageRequest.of("오늘 날씨 너무 좋다!");
            statusMessageService.createStatusMessage(1L, request);

            ArgumentCaptor<StatusMessage> captor = ArgumentCaptor.forClass(StatusMessage.class);
            verify(statusMessageRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals(1L, captor.getValue().getUser().getUserId()),
                    () -> assertEquals("오늘 날씨 너무 좋다!", captor.getValue().getContent())
            );
        }

        @Test
        @DisplayName("TC-1-2. 만료된 메시지 있을 때 작성 성공 (기존 메시지 soft delete)")
        void successWithExpiredMessage() {
            StatusMessage expiredMessage = StatusMessage.createForTest(
                    1L, user, "만료된 메시지", LocalDateTime.now().minusHours(25));
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.of(expiredMessage));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            CreateStatusMessageRequest request = CreateStatusMessageRequest.of("새 메시지");
            statusMessageService.createStatusMessage(1L, request);

            assertTrue(expiredMessage.isDeleted());
            ArgumentCaptor<StatusMessage> captor = ArgumentCaptor.forClass(StatusMessage.class);
            verify(statusMessageRepository).save(captor.capture());
            assertEquals("새 메시지", captor.getValue().getContent());
        }
    }

    @Nested
    @DisplayName("2. createStatusMessage() - 실패")
    class CreateStatusMessageFailCases {

        @Test
        @DisplayName("TC-2-1. 만료되지 않은 메시지 존재 → STATUS_MESSAGE_ALREADY_EXISTS")
        void failAlreadyExists() {
            StatusMessage existing = StatusMessage.createForTest(
                    1L, user, "기존 메시지", LocalDateTime.now().minusHours(1));
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

            CreateStatusMessageRequest request = CreateStatusMessageRequest.of("새 메시지");
            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.createStatusMessage(1L, request));

            assertEquals(ErrorCode.STATUS_MESSAGE_ALREADY_EXISTS, ex.getErrorCode());
            verify(statusMessageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("3. updateStatusMessage() - 성공")
    class UpdateStatusMessageSuccessCases {

        @Test
        @DisplayName("TC-3-1. 수정 성공")
        void success() {
            StatusMessage statusMessage = StatusMessage.createForTest(
                    1L, user, "기존 메시지", LocalDateTime.now().minusHours(1));
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.of(statusMessage));

            UpdateStatusMessageRequest request = UpdateStatusMessageRequest.of("수정된 메시지");
            statusMessageService.updateStatusMessage(1L, request);

            assertEquals("수정된 메시지", statusMessage.getContent());
        }
    }

    @Nested
    @DisplayName("4. updateStatusMessage() - 실패")
    class UpdateStatusMessageFailCases {

        @Test
        @DisplayName("TC-4-1. 상태메시지 없음 → STATUS_MESSAGE_NOT_FOUND")
        void failNotFound() {
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.empty());

            UpdateStatusMessageRequest request = UpdateStatusMessageRequest.of("수정된 메시지");
            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.updateStatusMessage(1L, request));

            assertEquals(ErrorCode.STATUS_MESSAGE_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-4-2. 24시간 만료 → STATUS_MESSAGE_EXPIRED")
        void failExpired() {
            StatusMessage statusMessage = StatusMessage.createForTest(
                    1L, user, "기존 메시지", LocalDateTime.now().minusHours(25));
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.of(statusMessage));

            UpdateStatusMessageRequest request = UpdateStatusMessageRequest.of("수정된 메시지");
            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.updateStatusMessage(1L, request));

            assertEquals(ErrorCode.STATUS_MESSAGE_EXPIRED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("5. deleteStatusMessage() - 성공")
    class DeleteStatusMessageSuccessCases {

        @Test
        @DisplayName("TC-5-1. 삭제 성공 (soft delete)")
        void success() {
            StatusMessage statusMessage = StatusMessage.createForTest(
                    1L, user, "기존 메시지", LocalDateTime.now().minusHours(1));
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.of(statusMessage));

            statusMessageService.deleteStatusMessage(1L);

            assertTrue(statusMessage.isDeleted());
        }
    }

    @Nested
    @DisplayName("6. deleteStatusMessage() - 실패")
    class DeleteStatusMessageFailCases {

        @Test
        @DisplayName("TC-6-1. 상태메시지 없음 → STATUS_MESSAGE_NOT_FOUND")
        void failNotFound() {
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.deleteStatusMessage(1L));

            assertEquals(ErrorCode.STATUS_MESSAGE_NOT_FOUND, ex.getErrorCode());
        }
    }
}
