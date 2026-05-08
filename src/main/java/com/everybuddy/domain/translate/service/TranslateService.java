package com.everybuddy.domain.translate.service;

import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.util.EnumConverter;
import com.everybuddy.domain.translate.client.TritonClient;
import com.everybuddy.domain.translate.client.TritonClient.SpeechTranslationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateService {

    private static final long MAX_AUDIO_SIZE = 50L * 1024 * 1024;
    private static final Set<String> SUPPORTED_AUDIO_TYPES = Set.of(
            "audio/wav", "audio/wave", "audio/x-wav",
            "audio/mpeg", "audio/mp3",
            "audio/ogg",
            "audio/webm",
            "audio/mp4",
            "audio/x-m4a", "audio/m4a"
    );

    private final TritonClient tritonClient;

    public TextTranslateResponse translateText(TextTranslateRequest request) {
        Language targetLanguage = EnumConverter.stringToEnum(
                request.getTargetLang(), Language.class, ErrorCode.UNSUPPORTED_LANGUAGE);
        Language sourceLanguage = EnumConverter.stringToEnum(
                request.getSourceLang(), Language.class, ErrorCode.UNSUPPORTED_LANGUAGE);

        String translatedText = tritonClient.translateText(
                request.getText(),
                sourceLanguage.getCode(),
                targetLanguage.getCode()
        );

        return TextTranslateResponse.of(
                translatedText,
                sourceLanguage.getCode(),
                targetLanguage.getCode()
        );
    }

    public SpeechTranslateResponse translateSpeech(MultipartFile file, String targetLang) {
        validateAudioFile(file);
        Language targetLanguage = EnumConverter.stringToEnum(
                targetLang, Language.class, ErrorCode.UNSUPPORTED_LANGUAGE);

        byte[] audioBytes;
        try {
            audioBytes = file.getBytes();
        } catch (IOException e) {
            log.error("오디오 파일 읽기 실패", e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        SpeechTranslationResult result = tritonClient.translateSpeech(
                audioBytes,
                targetLanguage.getCode()
        );

        return SpeechTranslateResponse.of(
                result.sourceText(),
                result.translatedText(),
                result.sourceLanguage(),
                targetLanguage.getCode()
        );
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw new CustomException(ErrorCode.AUDIO_FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_AUDIO_TYPES.contains(contentType.toLowerCase())) {
            throw new CustomException(ErrorCode.INVALID_AUDIO_FORMAT);
        }
    }
}
