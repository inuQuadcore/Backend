package com.everybuddy.domain.translate.service;

import com.everybuddy.domain.translate.client.TritonClient;
import com.everybuddy.domain.translate.client.TritonClient.SpeechTranslationResult;
import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.domain.translate.dto.TtsRequest;
import com.everybuddy.domain.translate.dto.VideoTranslateResponse;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.util.FfmpegMediaConverter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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

    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;
    private static final Set<String> SUPPORTED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/quicktime"   // .mov
    );

    // SEGMENTS_JSON 파싱 전용 ObjectMapper (field visibility ANY + snake_case 매핑)
    private static final ObjectMapper VIDEO_OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private final TritonClient tritonClient;
    private final UserLanguageRepository userLanguageRepository;
    private final FfmpegMediaConverter ffmpegConverter;

    public TextTranslateResponse translateText(TextTranslateRequest request, Long userId) {
        String targetCode = resolvePrimaryLanguageCode(userId);

        String translatedText = tritonClient.translateText(request.getText(), targetCode);

        return TextTranslateResponse.of(translatedText);
    }

    public SpeechTranslateResponse translateSpeech(MultipartFile file, Long userId) {
        validateAudioFile(file);
        String targetCode = resolvePrimaryLanguageCode(userId);

        byte[] audioBytes;
        try {
            audioBytes = file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.MULTIPART_READ_FAILED, e);
        }

        SpeechTranslationResult result = tritonClient.translateSpeech(audioBytes, targetCode);

        return SpeechTranslateResponse.of(result.sourceText(), result.translatedText());
    }

    public byte[] tts(TtsRequest request) {
        return tritonClient.synthesizeSpeech(request.getText(), request.getLanguage(), request.getVoice());
    }

    public VideoTranslateResponse translateVideo(MultipartFile file, Long userId) {
        validateVideoFile(file);
        String targetCode = resolvePrimaryLanguageCode(userId);

        byte[] videoBytes;
        try {
            videoBytes = file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.MULTIPART_READ_FAILED, e);
        }

        byte[] wavBytes = ffmpegConverter.convertVideoToWav(videoBytes);
        TritonClient.VideoTranslationResult result = tritonClient.translateVideoSpeech(wavBytes, targetCode);

        try {
            SegmentsJson parsed = VIDEO_OBJECT_MAPPER.readValue(result.segmentsJson(), SegmentsJson.class);
            List<VideoTranslateResponse.Segment> segments = parsed.segments.stream()
                    .map(s -> new VideoTranslateResponse.Segment(
                            s.index, s.startSeconds, s.endSeconds,
                            s.timestamp, s.sourceText, s.sourceLanguage,
                            s.translatedText, s.inferenceSeconds))
                    .toList();
            return VideoTranslateResponse.of(result.translatedText(), segments);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.MODEL_ERROR, e);
        }
    }

    private String resolvePrimaryLanguageCode(Long userId) {
        UserLanguage primary = userLanguageRepository.findByUserUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_PRIMARY_LANGUAGE_NOT_FOUND));
        return primary.getLanguage().getCode();
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

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new CustomException(ErrorCode.VIDEO_FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
            throw new CustomException(ErrorCode.INVALID_VIDEO_FORMAT);
        }
    }

    // ── SEGMENTS_JSON 파싱용 내부 클래스 ──────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SegmentsJson {
        List<TritonSegment> segments = List.of();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TritonSegment {
        int index;
        double startSeconds;
        double endSeconds;
        String timestamp;
        String sourceText;
        String sourceLanguage;
        String translatedText;
        double inferenceSeconds;
    }
}
