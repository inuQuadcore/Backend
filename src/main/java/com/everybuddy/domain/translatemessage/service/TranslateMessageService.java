package com.everybuddy.domain.translatemessage.service;

import com.everybuddy.domain.translatemessage.dto.*;
import com.everybuddy.domain.translatemessage.entity.TranslatedMessage;
import com.everybuddy.domain.translatemessage.entity.TranslationType;
import com.everybuddy.domain.translatemessage.repository.TranslatedMessageRepository;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class TranslateMessageService {

    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of("wav", "mp3", "ogg", "webm", "m4a");

    private final TranslatedMessageRepository translatedMessageRepository;
    private final UserRepository userRepository;
    private final RestClient fastApiRestClient;

    public TranslateTextResponse translateText(Long userId, TranslateTextRequest request) {
        Language sourceLang = Language.fromCode(request.getSourceLanguage())
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSLATE_UNSUPPORTED_LANGUAGE));
        Language targetLang = Language.fromCode(request.getTargetLanguage())
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSLATE_UNSUPPORTED_LANGUAGE));

        FastApiTextResponse fastApiResponse = callFastApiText(
                new FastApiTextRequest(request.getText(), sourceLang.getCode(), targetLang.getCode())
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        translatedMessageRepository.save(
                TranslatedMessage.of(user, sourceLang, targetLang,
                        request.getText(), fastApiResponse.getTranslatedText(), TranslationType.TEXT)
        );

        return new TranslateTextResponse(
                fastApiResponse.getTranslatedText(),
                request.getSourceLanguage(),
                request.getTargetLanguage()
        );
    }

    public TranslateSpeechResponse translateSpeech(Long userId, String sourceLanguageCode,
                                                    String targetLanguageCode, MultipartFile audio) {
        validateAudioFile(audio);

        Language sourceLang = Language.fromCode(sourceLanguageCode)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSLATE_UNSUPPORTED_LANGUAGE));
        Language targetLang = Language.fromCode(targetLanguageCode)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSLATE_UNSUPPORTED_LANGUAGE));

        FastApiSpeechResponse fastApiResponse = callFastApiSpeech(
                audio, sourceLang.getCode(), targetLang.getCode()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        translatedMessageRepository.save(
                TranslatedMessage.of(user, sourceLang, targetLang,
                        fastApiResponse.getOriginalText(), fastApiResponse.getTranslatedText(), TranslationType.SPEECH)
        );

        return new TranslateSpeechResponse(
                fastApiResponse.getOriginalText(),
                fastApiResponse.getTranslatedText(),
                sourceLanguageCode,
                targetLanguageCode
        );
    }

    private FastApiTextResponse callFastApiText(FastApiTextRequest request) {
        try {
            return fastApiRestClient.post()
                    .uri("/translate/text")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        FastApiErrorResponse errorResponse;
                        try {
                            byte[] body = res.getBody().readAllBytes();
                            errorResponse = new com.fasterxml.jackson.databind.ObjectMapper()
                                    .readValue(body, FastApiErrorResponse.class);
                        } catch (IOException e) {
                            throw new CustomException(ErrorCode.TRANSLATE_SERVICE_UNAVAILABLE);
                        }
                        throw new CustomException(mapFastApiError(errorResponse.getError()));
                    })
                    .body(FastApiTextResponse.class);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new CustomException(ErrorCode.TRANSLATE_TIMEOUT);
            }
            throw new CustomException(ErrorCode.TRANSLATE_SERVICE_UNAVAILABLE);
        }
    }

    private FastApiSpeechResponse callFastApiSpeech(MultipartFile audio, String sourceLang, String targetLang) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio", audio.getResource());
            body.add("source_language", sourceLang);
            body.add("target_language", targetLang);

            return fastApiRestClient.post()
                    .uri("/translate/speech")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        FastApiErrorResponse errorResponse;
                        try {
                            byte[] bytes = res.getBody().readAllBytes();
                            errorResponse = new com.fasterxml.jackson.databind.ObjectMapper()
                                    .readValue(bytes, FastApiErrorResponse.class);
                        } catch (IOException e) {
                            throw new CustomException(ErrorCode.TRANSLATE_SERVICE_UNAVAILABLE);
                        }
                        throw new CustomException(mapFastApiError(errorResponse.getError()));
                    })
                    .body(FastApiSpeechResponse.class);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new CustomException(ErrorCode.TRANSLATE_TIMEOUT);
            }
            throw new CustomException(ErrorCode.TRANSLATE_SERVICE_UNAVAILABLE);
        }
    }

    private ErrorCode mapFastApiError(String error) {
        if (error == null) return ErrorCode.TRANSLATE_SERVICE_UNAVAILABLE;
        return switch (error) {
            case "unsupported_language" -> ErrorCode.TRANSLATE_UNSUPPORTED_LANGUAGE;
            case "invalid_audio_format" -> ErrorCode.TRANSLATE_INVALID_AUDIO_FORMAT;
            case "file_too_large" -> ErrorCode.TRANSLATE_FILE_SIZE_EXCEEDED;
            case "model_timeout" -> ErrorCode.TRANSLATE_TIMEOUT;
            default -> ErrorCode.TRANSLATE_SERVICE_UNAVAILABLE;
        };
    }

    private void validateAudioFile(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        String originalFilename = audio.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new CustomException(ErrorCode.TRANSLATE_INVALID_AUDIO_FORMAT);
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_AUDIO_EXTENSIONS.contains(ext)) {
            throw new CustomException(ErrorCode.TRANSLATE_INVALID_AUDIO_FORMAT);
        }
    }
}
