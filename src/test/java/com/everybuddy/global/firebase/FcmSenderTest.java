package com.everybuddy.global.firebase;

import com.everybuddy.domain.fcmtoken.entity.FcmToken;
import com.everybuddy.domain.fcmtoken.repository.FcmTokenRepository;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmSender 단위 테스트")
class FcmSenderTest {

    @Mock private FcmTokenRepository fcmTokenRepository;

    @InjectMocks
    private FcmSender fcmSender;

    private User user1;
    private User user2;
    private NotificationContent content;

    @BeforeEach
    void setUp() {
        user1 = User.createForTest(1L, "user1", "유저1", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        user2 = User.createForTest(2L, "user2", "유저2", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1991, 1, 1));
        content = NotificationContent.of("새로운 친구", "유저1님이 친구로 추가했어요.");
    }

    @Nested
    @DisplayName("early return")
    class EarlyReturn {

        @Test
        @DisplayName("userIds 비어있으면 FCM 호출 X, 토큰 조회도 X")
        void noopWhenEmptyUserIds() {
            try (MockedStatic<FirebaseMessaging> mocked = Mockito.mockStatic(FirebaseMessaging.class)) {
                fcmSender.sendToUsers(List.of(), content, Map.of());

                mocked.verifyNoInteractions();
                verify(fcmTokenRepository, never()).findAllByUserIdIn(any());
            }
        }

        @Test
        @DisplayName("등록된 토큰이 없으면 FCM 호출 X")
        void noopWhenNoTokens() {
            when(fcmTokenRepository.findAllByUserIdIn(List.of(1L))).thenReturn(List.of());

            try (MockedStatic<FirebaseMessaging> mocked = Mockito.mockStatic(FirebaseMessaging.class)) {
                fcmSender.sendToUsers(List.of(1L), content, Map.of());

                mocked.verifyNoInteractions();
            }
        }
    }

    @Nested
    @DisplayName("정상 발송")
    class Normal {

        @Test
        @DisplayName("등록된 토큰들로 FCM 멀티캐스트 발송")
        void sendsMulticast() throws FirebaseMessagingException {
            FcmToken token1 = FcmToken.createForTest(10L, user1, "token-1");
            FcmToken token2 = FcmToken.createForTest(11L, user2, "token-2");
            when(fcmTokenRepository.findAllByUserIdIn(List.of(1L, 2L))).thenReturn(List.of(token1, token2));

            FirebaseMessaging messaging = mock(FirebaseMessaging.class);
            BatchResponse batchResponse = mock(BatchResponse.class);
            SendResponse success = mock(SendResponse.class);
            when(success.isSuccessful()).thenReturn(true);
            when(batchResponse.getResponses()).thenReturn(List.of(success, success));
            when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

            try (MockedStatic<FirebaseMessaging> mocked = Mockito.mockStatic(FirebaseMessaging.class)) {
                mocked.when(FirebaseMessaging::getInstance).thenReturn(messaging);

                fcmSender.sendToUsers(List.of(1L, 2L), content, Map.of("type", "FRIEND_ADDED"));
            }

            verify(messaging).sendEachForMulticast(any(MulticastMessage.class));
            verify(fcmTokenRepository, never()).deleteByToken(any());
        }
    }

    @Nested
    @DisplayName("무효 토큰 정리")
    class InvalidTokenCleanup {

        @Test
        @DisplayName("UNREGISTERED 응답 토큰은 deleteByToken")
        void deletesUnregisteredToken() throws FirebaseMessagingException {
            FcmToken token1 = FcmToken.createForTest(10L, user1, "valid-token");
            FcmToken token2 = FcmToken.createForTest(11L, user2, "expired-token");
            when(fcmTokenRepository.findAllByUserIdIn(List.of(1L, 2L))).thenReturn(List.of(token1, token2));

            FirebaseMessaging messaging = mock(FirebaseMessaging.class);
            SendResponse success = mock(SendResponse.class);
            when(success.isSuccessful()).thenReturn(true);
            SendResponse unregistered = stubFailed(MessagingErrorCode.UNREGISTERED);

            BatchResponse batchResponse = mock(BatchResponse.class);
            when(batchResponse.getResponses()).thenReturn(List.of(success, unregistered));
            when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

            try (MockedStatic<FirebaseMessaging> mocked = Mockito.mockStatic(FirebaseMessaging.class)) {
                mocked.when(FirebaseMessaging::getInstance).thenReturn(messaging);

                fcmSender.sendToUsers(List.of(1L, 2L), content, Map.of());
            }

            verify(fcmTokenRepository).deleteByToken("expired-token");
            verify(fcmTokenRepository, never()).deleteByToken("valid-token");
        }

        @Test
        @DisplayName("INVALID_ARGUMENT 응답 토큰도 deleteByToken")
        void deletesInvalidArgumentToken() throws FirebaseMessagingException {
            FcmToken token = FcmToken.createForTest(10L, user1, "invalid-token");
            when(fcmTokenRepository.findAllByUserIdIn(List.of(1L))).thenReturn(List.of(token));

            FirebaseMessaging messaging = mock(FirebaseMessaging.class);
            SendResponse failed = stubFailed(MessagingErrorCode.INVALID_ARGUMENT);
            BatchResponse batchResponse = mock(BatchResponse.class);
            when(batchResponse.getResponses()).thenReturn(List.of(failed));
            when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

            try (MockedStatic<FirebaseMessaging> mocked = Mockito.mockStatic(FirebaseMessaging.class)) {
                mocked.when(FirebaseMessaging::getInstance).thenReturn(messaging);

                fcmSender.sendToUsers(List.of(1L), content, Map.of());
            }

            verify(fcmTokenRepository).deleteByToken("invalid-token");
        }

        @Test
        @DisplayName("다른 에러 코드는 토큰 보존")
        void preservesTokenOnOtherErrors() throws FirebaseMessagingException {
            FcmToken token = FcmToken.createForTest(10L, user1, "transient-error-token");
            when(fcmTokenRepository.findAllByUserIdIn(List.of(1L))).thenReturn(List.of(token));

            FirebaseMessaging messaging = mock(FirebaseMessaging.class);
            SendResponse failed = stubFailed(MessagingErrorCode.INTERNAL);
            BatchResponse batchResponse = mock(BatchResponse.class);
            when(batchResponse.getResponses()).thenReturn(List.of(failed));
            when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

            try (MockedStatic<FirebaseMessaging> mocked = Mockito.mockStatic(FirebaseMessaging.class)) {
                mocked.when(FirebaseMessaging::getInstance).thenReturn(messaging);

                fcmSender.sendToUsers(List.of(1L), content, Map.of());
            }

            verify(fcmTokenRepository, never()).deleteByToken(any());
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("FirebaseMessagingException 발생해도 호출자에게 전파하지 않음 (로그만)")
        void swallowsFirebaseException() throws FirebaseMessagingException {
            FcmToken token = FcmToken.createForTest(10L, user1, "token");
            when(fcmTokenRepository.findAllByUserIdIn(List.of(1L))).thenReturn(List.of(token));

            FirebaseMessaging messaging = mock(FirebaseMessaging.class);
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenThrow(exception);

            try (MockedStatic<FirebaseMessaging> mocked = Mockito.mockStatic(FirebaseMessaging.class)) {
                mocked.when(FirebaseMessaging::getInstance).thenReturn(messaging);

                assertDoesNotThrow(() -> fcmSender.sendToUsers(List.of(1L), content, Map.of()));
            }
        }
    }

    private SendResponse stubFailed(MessagingErrorCode code) {
        SendResponse response = mock(SendResponse.class);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(response.isSuccessful()).thenReturn(false);
        when(response.getException()).thenReturn(exception);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        return response;
    }
}
