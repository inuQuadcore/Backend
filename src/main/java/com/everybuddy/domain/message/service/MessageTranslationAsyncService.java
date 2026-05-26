package com.everybuddy.domain.message.service;

import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.entity.MediaType;
import com.everybuddy.domain.media.entity.TranslationStatus;
import com.everybuddy.domain.media.repository.MediaRepository;
import com.everybuddy.domain.message.event.TranslationRequestedEvent;
import com.everybuddy.domain.message.event.TranslationStatusChangedEvent;
import com.everybuddy.domain.translate.client.TritonClient;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.s3.service.StorageService;
import com.everybuddy.global.util.FfmpegMediaConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 음성·영상 메시지 비동기 번역 처리 서비스.
 *
 * <pre>
 * 흐름:
 * 1. MessageTranslationService가 PENDING 전이 후 TranslationRequestedEvent 발행
 * 2. 본 클래스의 handleTranslationRequested()가 커밋 후(@AFTER_COMMIT) 별도 스레드에서 실행
 * 3. S3 다운로드 → (영상이면 ffmpeg 변환) → Triton 호출 → DB COMPLETED/FAILED 전이
 * 4. TranslationStatusChangedEvent 발행 → ChatRtdbEventHandler가 Firebase 업데이트
 * </pre>
 *
 * DB 연결을 Triton 대기 시간(최대 120s) 동안 점유하지 않도록
 * S3 다운로드·Triton 호출 구간에서는 트랜잭션을 닫고 별도 {@code @Transactional} 로
 * DB를 업데이트합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageTranslationAsyncService {

    private final MediaRepository          mediaRepository;
    private final StorageService           storageService;
    private final TritonClient             tritonClient;
    private final FfmpegMediaConverter     ffmpegConverter;
    private final ApplicationEventPublisher eventPublisher;

    @Async("translationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTranslationRequested(TranslationRequestedEvent event) {
        Long mediaId    = event.getMediaId();
        Long messageId  = event.getMessageId();
        Long chatRoomId = event.getChatRoomId();
        MediaType mediaType = event.getMediaType();

        log.info("[번역] 시작 messageId={} mediaId={} mediaType={}", messageId, mediaId, mediaType);

        // ── Step 1. 미디어 정보 조회 (짧은 트랜잭션) ───────────────────────
        MediaSnapshot snapshot = loadSnapshot(mediaId);
        if (snapshot == null) {
            log.warn("[번역] media를 찾을 수 없어 중단 mediaId={}", mediaId);
            return;
        }

        // ── Step 2. S3 다운로드 (트랜잭션 외부) ────────────────────────────
        byte[] fileBytes;
        try {
            fileBytes = storageService.downloadFile(snapshot.fileKey());
        } catch (Exception e) {
            log.error("[번역] S3 다운로드 실패 mediaId={}", mediaId, e);
            saveFailedAndNotify(mediaId, messageId, chatRoomId);
            return;
        }

        // ── Step 3. Triton 추론 (트랜잭션 외부, 최대 120s) ─────────────────
        String translatedText;
        String segmentsJson = null;
        try {
            if (mediaType == MediaType.VIDEO) {
                byte[] wavBytes = ffmpegConverter.convertVideoToWav(fileBytes);
                TritonClient.VideoTranslationResult result =
                        tritonClient.translateVideoSpeech(wavBytes, snapshot.targetLanguage());
                translatedText = result.translatedText();
                segmentsJson   = result.segmentsJson();
            } else {
                // AUDIO
                TritonClient.SpeechTranslationResult result =
                        tritonClient.translateSpeech(fileBytes, snapshot.targetLanguage());
                translatedText = result.translatedText();
            }
        } catch (CustomException e) {
            log.error("[번역] Triton 오류 mediaId={} code={}", mediaId, e.getErrorCode(), e);
            saveFailedAndNotify(mediaId, messageId, chatRoomId);
            return;
        } catch (Exception e) {
            log.error("[번역] 예기치 못한 오류 mediaId={}", mediaId, e);
            saveFailedAndNotify(mediaId, messageId, chatRoomId);
            return;
        }

        // ── Step 4. 결과 저장 및 COMPLETED 이벤트 발행 ─────────────────────
        saveCompletedAndNotify(mediaId, messageId, chatRoomId, translatedText, segmentsJson);
        log.info("[번역] 완료 messageId={} mediaId={}", messageId, mediaId);
    }

    // ── 짧은 트랜잭션으로 미디어 정보 조회 ─────────────────────────────────────

    @Transactional(readOnly = true)
    public MediaSnapshot loadSnapshot(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .map(m -> new MediaSnapshot(m.getFileKey(), m.getTargetLanguage(), m.getMediaType()))
                .orElse(null);
    }

    // ── COMPLETED 저장 + 이벤트 발행 ────────────────────────────────────────

    @Transactional
    public void saveCompletedAndNotify(Long mediaId, Long messageId, Long chatRoomId,
                                       String translatedText, String segmentsJson) {
        mediaRepository.findById(mediaId).ifPresent(media -> {
            media.completeTranslation(translatedText, segmentsJson);
        });
        eventPublisher.publishEvent(
                TranslationStatusChangedEvent.of(messageId, chatRoomId, TranslationStatus.COMPLETED));
    }

    // ── FAILED 저장 + 이벤트 발행 ───────────────────────────────────────────

    @Transactional
    public void saveFailedAndNotify(Long mediaId, Long messageId, Long chatRoomId) {
        mediaRepository.findById(mediaId).ifPresent(Media::failTranslation);
        eventPublisher.publishEvent(
                TranslationStatusChangedEvent.of(messageId, chatRoomId, TranslationStatus.FAILED));
    }

    // ── 스냅샷 레코드 ────────────────────────────────────────────────────────

    public record MediaSnapshot(String fileKey, String targetLanguage, MediaType mediaType) {}
}
