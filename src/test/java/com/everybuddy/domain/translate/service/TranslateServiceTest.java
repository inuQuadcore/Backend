package com.everybuddy.domain.translate.service;

import com.everybuddy.domain.translate.client.TritonClient;
import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.stream.Stream;

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

    @Mock private TritonClient tritonClient;

    @InjectMocks
    private TranslateService translateService;

    @Nested
    @DisplayName("1. translateText() - 성공")
    class TranslateTextSuccessCases {

        @Test
        @DisplayName("TC-1-1. 정상 번역 - 응답 DTO에 번역 결과 매핑")
        void success() {
            TextTranslateRequest request = TextTranslateRequest.ofForTest("hello", "ENGLISH", "KOREAN");
            when(tritonClient.translateText("hello", "en", "ko")).thenReturn("안녕하세요");

            TextTranslateResponse response = translateService.translateText(request);

            assertEquals("안녕하세요", response.getTranslatedText());
        }
    }

    @Nested
    @DisplayName("2. translateText() - 실패")
    class TranslateTextFailCases {

        @ParameterizedTest(name = "TC-2-1.{index}. {0} → UNSUPPORTED_LANGUAGE")
        @MethodSource("invalidLanguageInputs")
        void failInvalidLanguage(String description, String sourceLang, String targetLang) {
            TextTranslateRequest request = TextTranslateRequest.ofForTest("hello", sourceLang, targetLang);

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateText(request));

            assertEquals(ErrorCode.UNSUPPORTED_LANGUAGE, ex.getErrorCode());
            verify(tritonClient, never()).translateText(any(), any(), any());
        }

        static Stream<Arguments> invalidLanguageInputs() {
            return Stream.of(
                    Arguments.of("targetLang가 enum에 없음", "ENGLISH", "INVALID_LANG"),
                    Arguments.of("sourceLang가 enum에 없음", "INVALID_LANG", "KOREAN")
            );
        }
    }

    @Nested
    @DisplayName("3. translateSpeech() - 성공")
    class TranslateSpeechSuccessCases {

        @Test
        @DisplayName("TC-3-1. 정상 번역 - 응답 DTO에 번역 결과 매핑")
        void success() throws IOException {
            byte[] audioBytes = "fake-audio-data".getBytes();
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn((long) audioBytes.length);
            when(file.getContentType()).thenReturn("audio/wav");
            when(file.getBytes()).thenReturn(audioBytes);

            when(tritonClient.translateSpeech(audioBytes, "ko")).thenReturn("안녕 세계");

            SpeechTranslateResponse response = translateService.translateSpeech(file, "KOREAN");

            assertEquals("안녕 세계", response.getTranslatedText());
        }
    }

    @Nested
    @DisplayName("4. translateSpeech() - 실패")
    class TranslateSpeechFailCases {

        @Test
        @DisplayName("TC-4-1. file이 null → EMPTY_FILE")
        void failNullFile() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(null, "KOREAN"));

            assertEquals(ErrorCode.EMPTY_FILE, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }

        @Test
        @DisplayName("TC-4-2. file.isEmpty()=true → EMPTY_FILE")
        void failEmptyFile() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(file, "KOREAN"));

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
                    () -> translateService.translateSpeech(file, "KOREAN"));

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
                    () -> translateService.translateSpeech(file, "KOREAN"));

            assertEquals(ErrorCode.INVALID_AUDIO_FORMAT, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }

        @Test
        @DisplayName("TC-4-5. targetLang가 enum에 없음 → UNSUPPORTED_LANGUAGE")
        void failInvalidLanguage() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(1024L);
            when(file.getContentType()).thenReturn("audio/wav");

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(file, "INVALID_LANG"));

            assertEquals(ErrorCode.UNSUPPORTED_LANGUAGE, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }

        @Test
        @DisplayName("TC-4-6. file.getBytes()가 IOException → FILE_UPLOAD_FAILED")
        void failBytesIOException() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(1024L);
            when(file.getContentType()).thenReturn("audio/wav");
            when(file.getBytes()).thenThrow(new IOException("read failed"));

            CustomException ex = assertThrows(CustomException.class,
                    () -> translateService.translateSpeech(file, "KOREAN"));

            assertEquals(ErrorCode.FILE_UPLOAD_FAILED, ex.getErrorCode());
            verify(tritonClient, never()).translateSpeech(any(), any());
        }
    }
}
