package com.everybuddy.domain.translate.service;

import com.everybuddy.domain.translate.client.TritonClient;
import com.everybuddy.domain.translate.client.TritonClient.SpeechTranslationResult;
import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranslateService 단위 테스트")
class TranslateServiceTest {

    private static final Long USER_ID = 1L;

    @Mock private TritonClient tritonClient;
    @Mock private UserLanguageRepository userLanguageRepository;

    @InjectMocks
    private TranslateService translateService;

    private User user;
    private UserLanguage primaryKorean;

    @BeforeEach
    void setUp() {
        user = User.createForTest(USER_ID, "loginId", "홍길동", "pw",
                Country.KOREA, Gender.MALE, LocalDate.of(2000, 1, 1));
        primaryKorean = UserLanguage.of(user, Language.KOREAN, 5, true);
    }

    @Nested
    @DisplayName("1. translateText() - 성공")
    class TranslateTextSuccessCases {

        @Test
        @DisplayName("TC-1-1. 정상 번역 - 사용자 주언어로 번역 결과 매핑")
        void success() {
            TextTranslateRequest request = TextTranslateRequest.ofForTest("hello");
            when(userLanguageRepository.findByUserUserIdAndIsPrimaryTrue(USER_ID))
                    .thenReturn(Optional.of(primaryKorean));
            when(tritonClient.translateText("hello", "ko")).thenReturn("안녕하세요");

            TextTranslateResponse response = translateService.translateText(request, USER_ID);

            assertEquals("안녕하세요", response.getTranslatedText());
        }
    }

    @Nested
    @DisplayName("2. translateText() - 실패")
    class TranslateTextFailCases {

        @Test
        @DisplayName("TC-2-1. 사용자 주언어 없음 → USER_PRIMARY_LANGUAGE_NOT_FOUND")
        void failNoPrimaryLanguage() {
            TextTranslateRequest request = TextTranslateRequest.ofForTest("hello");
            when(userLanguageRepository.findByUserUserIdAndIsPrimaryTrue(USER_ID))
                    .thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateText(request, USER_ID));

            assertEquals(ErrorCode.USER_PRIMARY_LANGUAGE_NOT_FOUND, ex.getErrorCode());
            verify(tritonClient, never()).translateText(any(), any());
        }
    }

    @Nested
    @DisplayName("3. translateSpeech() - 성공")
    class TranslateSpeechSuccessCases {

        @Test
        @DisplayName("TC-3-1. 정상 번역 - 사용자 주언어로 STT 원문과 번역 결과 매핑")
        void success() throws IOException {
            byte[] audioBytes = "fake-audio-data".getBytes();
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn((long) audioBytes.length);
            when(file.getContentType()).thenReturn("audio/wav");
            when(file.getBytes()).thenReturn(audioBytes);

            when(userLanguageRepository.findByUserUserIdAndIsPrimaryTrue(USER_ID))
                    .thenReturn(Optional.of(primaryKorean));
            when(tritonClient.translateSpeech(audioBytes, "ko"))
                    .thenReturn(new SpeechTranslationResult("Hello world", "안녕 세계"));

            SpeechTranslateResponse response = translateService.translateSpeech(file, USER_ID);

            assertAll(
                    () -> assertEquals("Hello world", response.getSourceText()),
                    () -> assertEquals("안녕 세계", response.getTranslatedText())
            );
        }
    }

    @Nested
    @DisplayName("4. translateSpeech() - 실패")
    class TranslateSpeechFailCases {

        @Test
        @DisplayName("TC-4-1. file이 null → EMPTY_FILE")
        void failNullFile() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(null, USER_ID));

            assertEquals(ErrorCode.EMPTY_FILE, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }

        @Test
        @DisplayName("TC-4-2. file.isEmpty()=true → EMPTY_FILE")
        void failEmptyFile() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(file, USER_ID));

            assertEquals(ErrorCode.EMPTY_FILE, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }

        @Test
        @DisplayName("TC-4-3. file 크기가 50MB 초과 → AUDIO_FILE_TOO_LARGE")
        void failTooLarge() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(50L * 1024 * 1024 + 1);

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(file, USER_ID));

            assertEquals(ErrorCode.AUDIO_FILE_TOO_LARGE, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }

        @ParameterizedTest(name = "TC-4-4.{index}. contentType={0} → INVALID_AUDIO_FORMAT")
        @NullSource
        @ValueSource(strings = {"video/mp4", "image/png", "application/json"})
        void failInvalidContentType(String contentType) {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(1024L);
            when(file.getContentType()).thenReturn(contentType);

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(file, USER_ID));

            assertEquals(ErrorCode.INVALID_AUDIO_FORMAT, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }

        @Test
        @DisplayName("TC-4-5. 사용자 주언어 없음 → USER_PRIMARY_LANGUAGE_NOT_FOUND")
        void failNoPrimaryLanguage() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(1024L);
            when(file.getContentType()).thenReturn("audio/wav");

            when(userLanguageRepository.findByUserUserIdAndIsPrimaryTrue(USER_ID))
                    .thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(file, USER_ID));

            assertEquals(ErrorCode.USER_PRIMARY_LANGUAGE_NOT_FOUND, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }

        @Test
        @DisplayName("TC-4-6. file.getBytes()가 IOException → MULTIPART_READ_FAILED")
        void failBytesIOException() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(1024L);
            when(file.getContentType()).thenReturn("audio/wav");
            when(file.getBytes()).thenThrow(new IOException("read failed"));

            when(userLanguageRepository.findByUserUserIdAndIsPrimaryTrue(USER_ID))
                    .thenReturn(Optional.of(primaryKorean));

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(file, USER_ID));

            assertEquals(ErrorCode.MULTIPART_READ_FAILED, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }
    }
}
