package com.everybuddy.domain.statusmessage.service;

import com.everybuddy.domain.statusmessage.dto.CreateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.dto.FriendStatusMessageListResponse;
import com.everybuddy.domain.statusmessage.dto.MyStatusMessageResponse;
import com.everybuddy.domain.statusmessage.dto.UpdateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import com.everybuddy.domain.statusmessage.repository.StatusMessageRepository;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatusMessageService 단위 테스트")
class StatusMessageServiceTest {

    @Mock private StatusMessageRepository statusMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;

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

            CreateStatusMessageRequest request = CreateStatusMessageRequest.ofForTest("오늘 날씨 너무 좋다!");
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

            CreateStatusMessageRequest request = CreateStatusMessageRequest.ofForTest("새 메시지");
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

            CreateStatusMessageRequest request = CreateStatusMessageRequest.ofForTest("새 메시지");
            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.createStatusMessage(1L, request));

            assertEquals(ErrorCode.STATUS_MESSAGE_ALREADY_EXISTS, ex.getErrorCode());
            verify(statusMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-2. 탈퇴한 유저 → USER_DELETED, 저장 호출 안 됨")
        void failUserDeleted() {
            user.softDelete();
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            CreateStatusMessageRequest request = CreateStatusMessageRequest.ofForTest("새 메시지");
            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.createStatusMessage(1L, request));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(statusMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-3. 존재하지 않는 유저 → USER_NOT_FOUND, 저장 호출 안 됨")
        void failUserNotFound() {
            when(statusMessageRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            CreateStatusMessageRequest request = CreateStatusMessageRequest.ofForTest("새 메시지");
            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.createStatusMessage(1L, request));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
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

            UpdateStatusMessageRequest request = UpdateStatusMessageRequest.ofForTest("수정된 메시지");
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

            UpdateStatusMessageRequest request = UpdateStatusMessageRequest.ofForTest("수정된 메시지");
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

            UpdateStatusMessageRequest request = UpdateStatusMessageRequest.ofForTest("수정된 메시지");
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

    @Nested
    @DisplayName("7. getMyStatusMessage() - 성공")
    class GetMyStatusMessageSuccessCases {

        @Test
        @DisplayName("TC-7-1. profile 있을 때 → profileImageUrl 포함 응답")
        void successWithProfile() {
            user.updateProfile(null, null, null, null, null, "profiles/user-1/img.jpg");
            StatusMessage statusMessage = StatusMessage.createForTest(
                    10L, user, "오늘 기분 좋다", LocalDateTime.now().minusMinutes(30));
            when(statusMessageRepository.findByUserIdWithUser(1L)).thenReturn(Optional.of(statusMessage));
            when(storageService.getPublicUrl("profiles/user-1/img.jpg")).thenReturn("https://cdn.example.com/img.jpg");

            MyStatusMessageResponse response = statusMessageService.getMyStatusMessage(1L);

            assertAll(
                    () -> assertEquals(10L, response.getStatusMessageId()),
                    () -> assertEquals("홍길동", response.getNickname()),
                    () -> assertEquals("오늘 기분 좋다", response.getContent()),
                    () -> assertEquals("https://cdn.example.com/img.jpg", response.getProfileImageUrl())
            );
        }

        @Test
        @DisplayName("TC-7-2. profile null일 때 → profileImageUrl = null, storageService 미호출")
        void successWithoutProfile() {
            StatusMessage statusMessage = StatusMessage.createForTest(
                    10L, user, "오늘 기분 좋다", LocalDateTime.now().minusMinutes(30));
            when(statusMessageRepository.findByUserIdWithUser(1L)).thenReturn(Optional.of(statusMessage));

            MyStatusMessageResponse response = statusMessageService.getMyStatusMessage(1L);

            assertNull(response.getProfileImageUrl());
            verify(storageService, never()).getPublicUrl(any());
        }
    }

    @Nested
    @DisplayName("8. getMyStatusMessage() - 실패")
    class GetMyStatusMessageFailCases {

        @Test
        @DisplayName("TC-8-1. 상태메시지 없음 → STATUS_MESSAGE_NOT_FOUND")
        void failNotFound() {
            when(statusMessageRepository.findByUserIdWithUser(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.getMyStatusMessage(1L));

            assertEquals(ErrorCode.STATUS_MESSAGE_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("9. getFriendStatusMessages() - 성공")
    class GetFriendStatusMessagesSuccessCases {

        @Test
        @DisplayName("TC-9-1. cursor null, 결과 size 이하 → hasNext=false, nextCursor=마지막 ID")
        void successNoCursorNoNext() {
            StatusMessage sm1 = StatusMessage.createForTest(1L, user, "메시지1", LocalDateTime.now().minusMinutes(10));
            StatusMessage sm2 = StatusMessage.createForTest(2L, user, "메시지2", LocalDateTime.now().minusMinutes(20));
            when(statusMessageRepository.findFriendStatusMessages(1L, PageRequest.of(0, 21)))
                    .thenReturn(List.of(sm1, sm2));

            FriendStatusMessageListResponse response = statusMessageService.getFriendStatusMessages(1L, null, 20);

            assertAll(
                    () -> assertEquals(2, response.getStatusMessages().size()),
                    () -> assertFalse(response.isHasNext()),
                    () -> assertEquals(2L, response.getNextCursor())
            );
        }

        @Test
        @DisplayName("TC-9-2. cursor null, 결과 size+1개 → hasNext=true, nextCursor=마지막 ID")
        void successNoCursorHasNext() {
            List<StatusMessage> messages = new java.util.ArrayList<>();
            for (int i = 1; i <= 21; i++) {
                messages.add(StatusMessage.createForTest((long) i, user, "메시지" + i,
                        LocalDateTime.now().minusMinutes(i)));
            }
            when(statusMessageRepository.findFriendStatusMessages(1L, PageRequest.of(0, 21)))
                    .thenReturn(messages);

            FriendStatusMessageListResponse response = statusMessageService.getFriendStatusMessages(1L, null, 20);

            assertAll(
                    () -> assertEquals(20, response.getStatusMessages().size()),
                    () -> assertTrue(response.isHasNext()),
                    () -> assertEquals(20L, response.getNextCursor())
            );
        }

        @Test
        @DisplayName("TC-9-3. cursor 있을 때 → findFriendStatusMessagesAfterCursor 호출")
        void successWithCursor() {
            LocalDateTime cursorUpdatedAt = LocalDateTime.now().minusMinutes(30);
            StatusMessage cursorMessage = StatusMessage.createForTest(5L, user, "커서 메시지", cursorUpdatedAt);
            StatusMessage sm = StatusMessage.createForTest(6L, user, "이후 메시지", LocalDateTime.now().minusMinutes(40));
            when(statusMessageRepository.findById(5L)).thenReturn(Optional.of(cursorMessage));
            when(statusMessageRepository.findFriendStatusMessagesAfterCursor(1L, cursorUpdatedAt, 5L, PageRequest.of(0, 21)))
                    .thenReturn(List.of(sm));

            FriendStatusMessageListResponse response = statusMessageService.getFriendStatusMessages(1L, 5L, 20);

            assertAll(
                    () -> assertEquals(1, response.getStatusMessages().size()),
                    () -> assertFalse(response.isHasNext()),
                    () -> assertEquals(6L, response.getNextCursor())
            );
            verify(statusMessageRepository).findFriendStatusMessagesAfterCursor(1L, cursorUpdatedAt, 5L, PageRequest.of(0, 21));
        }
    }

    @Nested
    @DisplayName("10. getFriendStatusMessages() - 실패")
    class GetFriendStatusMessagesFailCases {

        @Test
        @DisplayName("TC-10-1. cursor에 해당하는 상태메시지 없음 → STATUS_MESSAGE_NOT_FOUND")
        void failCursorNotFound() {
            when(statusMessageRepository.findById(99L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> statusMessageService.getFriendStatusMessages(1L, 99L, 20));

            assertEquals(ErrorCode.STATUS_MESSAGE_NOT_FOUND, ex.getErrorCode());
        }
    }
}
