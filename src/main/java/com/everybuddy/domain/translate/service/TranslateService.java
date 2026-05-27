package com.everybuddy.domain.translate.service;

import com.everybuddy.domain.translate.client.TritonClient;
import com.everybuddy.domain.translate.client.TritonClient.SpeechTranslationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.domain.translate.dto.TtsRequest;
import com.everybuddy.domain.translate.dto.VideoTranslateResponse;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
import java.util.stream.Collectors;

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

        // MP4/MOV 원본 바이트를 그대로 Triton에 전송.
        // Triton gemma_v2tt 모델 내부 ffmpeg이 magic bytes로 포맷을 자동 감지한다.
        byte[] videoBytes;
        try {
            videoBytes = file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.MULTIPART_READ_FAILED, e);
        }

        // gemma_v2tt는 SEGMENTS_JSON(raw 배열)만 반환. TRANSLATED_TEXT 출력 없음.
        String segmentsJson = tritonClient.translateVideoSpeech(videoBytes, targetCode);

        try {
            List<TritonSegment> tritonSegments = VIDEO_OBJECT_MAPPER.readValue(
                    segmentsJson, new TypeReference<List<TritonSegment>>() {});

            // 무음 영상: Triton이 [{index:0, total_segments:0, ...}] 더미 세그먼트를 반환 → 필터링
            List<VideoTranslateResponse.Segment> segments = tritonSegments.stream()
                    .filter(s -> s.totalSegments > 0)
                    .map(s -> new VideoTranslateResponse.Segment(
                            s.index, s.totalSegments, s.startSeconds, s.endSeconds,
                            s.timestamp, s.sourceText, s.sourceLanguage,
                            s.translatedText, s.inferenceSeconds))
                    .toList();

            // translatedText: 세그먼트 번역문을 공백으로 이어붙여 파생
            String translatedText = segments.stream()
                    .map(VideoTranslateResponse.Segment::getTranslatedText)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining(" "));

            return VideoTranslateResponse.of(translatedText, segments);
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

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TritonSegment {
        int index;
        int totalSegments;      // 무음 더미 세그먼트 판별용 (0이면 무음)
        double startSeconds;
        double endSeconds;
        String timestamp;
        String sourceText;
        String sourceLanguage;
        String translatedText;
        double inferenceSeconds;
    }
}
