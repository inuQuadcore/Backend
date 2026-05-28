package com.everybuddy.domain.translate.service;

import com.everybuddy.domain.translate.client.TritonClient;
import com.everybuddy.domain.translate.client.TritonClient.SpeechTranslationResult;
import com.everybuddy.domain.translate.client.TritonClient.VideoPrepareResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.domain.translate.dto.TtsRequest;
import com.everybuddy.domain.translate.dto.VideoTranslateResponse;
import com.everybuddy.domain.translate.dto.sse.VideoCompletedSseEvent;
import com.everybuddy.domain.translate.dto.sse.VideoSegmentErrorSseEvent;
import com.everybuddy.domain.translate.dto.sse.VideoSegmentSseEvent;
import com.everybuddy.domain.translate.dto.sse.VideoStreamErrorSseEvent;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Slf4j
@Service
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

    /** 신뢰할 수 없는 source_language 값 목록 (모두 소문자, strip 후 비교). */
    private static final Set<String> UNRELIABLE_LANGUAGES = Set.of(
            "", "unknown", "undetected", "none", "n/a", "na", "null",
            "detected source language", "<detected source language>",
            "original-language", "<original-language>"
    );

    /** Triton 응답(snake_case JSON) 파싱 전용 ObjectMapper. */
    private static final ObjectMapper VIDEO_OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    /** SSE 이벤트 직렬화용 ObjectMapper — 기본 camelCase 출력. */
    private static final ObjectMapper SSE_MAPPER = new ObjectMapper();

    private final TritonClient tritonClient;
    private final UserLanguageRepository userLanguageRepository;
    private final Executor v2ttExecutor;

    public TranslateService(TritonClient tritonClient,
                            UserLanguageRepository userLanguageRepository,
                            @Qualifier("v2ttExecutor") Executor v2ttExecutor) {
        this.tritonClient = tritonClient;
        this.userLanguageRepository = userLanguageRepository;
        this.v2ttExecutor = v2ttExecutor;
    }

    // ── 기존 번역 메서드 ─────────────────────────────────────────────────────────

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

        String segmentsJson = tritonClient.translateVideoSpeech(videoBytes, targetCode);

        try {
            List<TritonSegment> tritonSegments = VIDEO_OBJECT_MAPPER.readValue(
                    segmentsJson, new TypeReference<List<TritonSegment>>() {});

            List<VideoTranslateResponse.Segment> segments = tritonSegments.stream()
                    .filter(s -> s.totalSegments > 0)
                    .map(s -> new VideoTranslateResponse.Segment(
                            s.index, s.totalSegments, s.startSeconds, s.endSeconds,
                            s.timestamp, s.sourceText, s.sourceLanguage,
                            s.translatedText, s.inferenceSeconds))
                    .toList();

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

    // ── V2TT 스트리밍 ────────────────────────────────────────────────────────────

    /**
     * 영상 파일을 검증·읽은 뒤 SseEmitter를 반환하고, 스트리밍 작업을 v2ttExecutor에 위임한다.
     * 파일 검증 실패는 emitter 생성 전에 CustomException을 던져 HTTP 4xx로 처리된다.
     */
    public SseEmitter streamTranslateVideo(MultipartFile file, Long userId) {
        validateVideoFile(file);

        byte[] videoBytes;
        try {
            videoBytes = file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.MULTIPART_READ_FAILED, e);
        }

        SseEmitter emitter = new SseEmitter(180_000L);
        v2ttExecutor.submit(() -> processVideoStream(videoBytes, userId, emitter));
        return emitter;
    }

    /**
     * V2TT 스트리밍 핵심 로직. v2ttExecutor 스레드에서 실행된다.
     *
     * <ol>
     *   <li>gemma_v2tt_prepare 호출 → job_id + timestamps 수신</li>
     *   <li>segment 1 동기 처리 → source_language 확정</li>
     *   <li>나머지 segments 비동기 병렬 처리(동시성 3, Semaphore 제어)</li>
     *   <li>index 순서 보장하여 SseEmitter.send()</li>
     *   <li>finally: gemma_v2tt_cleanup 호출(실패해도 예외 전파 안 함)</li>
     * </ol>
     */
    private void processVideoStream(byte[] videoBytes, Long userId, SseEmitter emitter) {
        String targetCode;
        try {
            targetCode = resolvePrimaryLanguageCode(userId);
        } catch (CustomException e) {
            sendStreamError(emitter, e);
            return;
        }

        String jobId = null;

        try {
            // ── 1. prepare ────────────────────────────────────────────────────────
            VideoPrepareResult prepare = tritonClient.prepareVideo(videoBytes);
            jobId = prepare.jobId();

            List<TritonSegment> segments = VIDEO_OBJECT_MAPPER.readValue(
                    prepare.segmentsJson(), new TypeReference<List<TritonSegment>>() {});

            int totalSegments = segments.size();

            if (totalSegments == 0) {
                sendSseEvent(emitter, new VideoCompletedSseEvent(0));
                emitter.complete();
                return;
            }

            // ── 2. segment 1 (동기 — source_language 확정용) ─────────────────────
            TritonSegment seg1 = segments.get(0);
            String seg1Json = tritonClient.translateSegment(
                    jobId,
                    String.valueOf(seg1.index),
                    String.valueOf(seg1.startSeconds),
                    String.valueOf(seg1.endSeconds),
                    targetCode, null);

            TritonSegment result1 = VIDEO_OBJECT_MAPPER.readValue(seg1Json, TritonSegment.class);
            String lockedLanguage = resolveLockedLanguage(result1.sourceLanguage);

            sendSseEvent(emitter, toSegmentEvent(result1));

            if (totalSegments == 1) {
                sendSseEvent(emitter, new VideoCompletedSseEvent(totalSegments));
                emitter.complete();
                return;
            }

            // ── 3. 나머지 segments 비동기 병렬 처리 + 순서 보장 버퍼 ──────────────
            Object bufferLock = new Object();
            Map<Integer, Object> buffer = new HashMap<>();
            int[] nextToSend = {2};

            Semaphore semaphore = new Semaphore(3); // Triton 동시 요청 최대 3
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 1; i < segments.size(); i++) {
                TritonSegment seg = segments.get(i);
                final String srcLang = lockedLanguage;
                final String finalJobId = jobId;

                CompletableFuture<Void> future = CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                semaphore.acquire();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new CompletionException(e);
                            }
                            try {
                                String json = tritonClient.translateSegment(
                                        finalJobId,
                                        String.valueOf(seg.index),
                                        String.valueOf(seg.startSeconds),
                                        String.valueOf(seg.endSeconds),
                                        targetCode, srcLang);
                                return VIDEO_OBJECT_MAPPER.readValue(json, TritonSegment.class);
                            } catch (Exception e) {
                                throw new CompletionException(e);
                            } finally {
                                semaphore.release();
                            }
                        }, v2ttExecutor)
                        .handle((result, ex) -> {
                            Object event = (ex != null)
                                    ? toSegmentErrorEvent(seg.index, ex)
                                    : toSegmentEvent(result);

                            synchronized (bufferLock) {
                                buffer.put(seg.index, event);
                                // 순서대로 연속 전송 가능한 이벤트 flush
                                while (buffer.containsKey(nextToSend[0])) {
                                    Object evt = buffer.remove(nextToSend[0]);
                                    try {
                                        sendSseEvent(emitter, evt);
                                    } catch (IOException ioEx) {
                                        log.warn("[v2tt] SSE send failed at index {}: {}",
                                                nextToSend[0], ioEx.getMessage());
                                        break;
                                    }
                                    nextToSend[0]++;
                                }
                            }
                            return null;
                        });

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            sendSseEvent(emitter, new VideoCompletedSseEvent(totalSegments));
            emitter.complete();

        } catch (Exception e) {
            log.warn("[v2tt] stream failed: {}", e.getMessage(), e);
            sendStreamError(emitter, e); // stream_error 이벤트 전송 후 completeWithError
        } finally {
            if (jobId != null) {
                tritonClient.cleanupJob(jobId); // 내부에서 예외 삼킴
            }
        }
    }

    // ── 헬퍼 메서드 ──────────────────────────────────────────────────────────────

    /** source_language 가 신뢰 불가 값이면 null 반환(Gemma 자동 감지로 폴백). */
    private String resolveLockedLanguage(String sourceLang) {
        if (sourceLang == null) return null;
        return UNRELIABLE_LANGUAGES.contains(sourceLang.strip().toLowerCase()) ? null : sourceLang;
    }

    private VideoSegmentSseEvent toSegmentEvent(TritonSegment seg) {
        return new VideoSegmentSseEvent(
                seg.index, seg.startSeconds, seg.endSeconds,
                seg.timestamp, seg.sourceText, seg.sourceLanguage,
                seg.translatedText, seg.inferenceSeconds);
    }

    private VideoSegmentErrorSseEvent toSegmentErrorEvent(int index, Throwable ex) {
        Throwable cause = (ex instanceof CompletionException && ex.getCause() != null)
                ? ex.getCause() : ex;
        if (cause instanceof CustomException ce) {
            ErrorCode ec = ce.getErrorCode();
            return new VideoSegmentErrorSseEvent(index, ec.getCode(), ec.name(), ec.getMessage());
        }
        return new VideoSegmentErrorSseEvent(
                index,
                ErrorCode.MODEL_ERROR.getCode(),
                ErrorCode.MODEL_ERROR.name(),
                ErrorCode.MODEL_ERROR.getMessage());
    }

    private VideoStreamErrorSseEvent toStreamErrorEvent(Throwable ex) {
        if (ex instanceof CustomException ce) {
            ErrorCode ec = ce.getErrorCode();
            return new VideoStreamErrorSseEvent(ec.getCode(), ec.name(), ec.getMessage());
        }
        return new VideoStreamErrorSseEvent(
                ErrorCode.MODEL_ERROR.getCode(),
                ErrorCode.MODEL_ERROR.name(),
                ErrorCode.MODEL_ERROR.getMessage());
    }

    private void sendSseEvent(SseEmitter emitter, Object event) throws IOException {
        emitter.send(SseEmitter.event().data(SSE_MAPPER.writeValueAsString(event)));
    }

    private void sendStreamError(SseEmitter emitter, Exception e) {
        try {
            sendSseEvent(emitter, toStreamErrorEvent(e));
        } catch (Exception ignored) {
            // emitter 가 이미 닫혔거나 직렬화 실패 — 무시
        }
        emitter.completeWithError(e);
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

    // ── Triton 응답 파싱용 내부 클래스 ───────────────────────────────────────────

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TritonSegment {
        int index;
        int totalSegments;
        double startSeconds;
        double endSeconds;
        String timestamp;
        String sourceText;
        String sourceLanguage;
        String translatedText;
        double inferenceSeconds;
    }
}
