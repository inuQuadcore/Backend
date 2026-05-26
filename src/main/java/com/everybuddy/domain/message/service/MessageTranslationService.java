package com.everybuddy.domain.message.service;

import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.entity.TranslationStatus;
import com.everybuddy.domain.message.dto.TranslationResultResponse;
import com.everybuddy.domain.message.dto.TranslationResultResponse.Segment;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.event.TranslationRequestedEvent;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageTranslationService {

    private static final ObjectMapper SEGMENT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final MessageRepository        messageRepository;
    private final ChatPartRepository       chatPartRepository;
    private final UserLanguageRepository   userLanguageRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 번역 요청 (POST /api/v1/messages/{messageId}/translate).
     *
     * <ul>
     *   <li>COMPLETED → 즉시 캐시 반환 (200)</li>
     *   <li>PENDING   → 처리 중 안내 (202)</li>
     *   <li>null·FAILED → PENDING 전이 후 비동기 Triton 호출 트리거 (202)</li>
     * </ul>
     */
    @Transactional
    public TranslationResultResponse requestTranslation(Long messageId, Long userId) {
        Message message = findMessage(messageId);
        validateChatParticipation(userId, message);

        Media media = message.getMedia();
        if (media == null || !media.isTranslatable()) {
            throw new CustomException(ErrorCode.MEDIA_NOT_TRANSLATABLE);
        }

        // ── 이미 완료된 경우: 캐시 즉시 반환 ─────────────────────────────────
        if (media.getTranslationStatus() == TranslationStatus.COMPLETED) {
            return buildCompletedResponse(media);
        }

        // ── 이미 PENDING: 다른 사용자가 먼저 요청함 ──────────────────────────
        if (media.getTranslationStatus() == TranslationStatus.PENDING) {
            return TranslationResultResponse.pending();
        }

        // ── null or FAILED: 새 번역 요청 시작 ────────────────────────────────
        String targetLanguage = resolvePrimaryLanguageCode(userId);
        media.markTranslationPending(targetLanguage);
        // @TransactionalEventListener(AFTER_COMMIT) 리스너가 커밋 후 Firebase·Triton 처리
        eventPublisher.publishEvent(TranslationRequestedEvent.of(message, media));

        return TranslationResultResponse.pending();
    }

    /**
     * 번역 결과 조회 (GET /api/v1/messages/{messageId}/translation).
     * PENDING이면 202, COMPLETED이면 200, 아직 요청 안 했으면 404.
     */
    @Transactional(readOnly = true)
    public TranslationResultResponse getTranslation(Long messageId, Long userId) {
        Message message = findMessage(messageId);
        validateChatParticipation(userId, message);

        Media media = message.getMedia();
        if (media == null || !media.isTranslatable()) {
            throw new CustomException(ErrorCode.MEDIA_NOT_TRANSLATABLE);
        }

        TranslationStatus status = media.getTranslationStatus();
        if (status == null) {
            throw new CustomException(ErrorCode.TRANSLATION_NOT_STARTED);
        }

        return switch (status) {
            case COMPLETED -> buildCompletedResponse(media);
            case PENDING   -> TranslationResultResponse.pending();
            case FAILED    -> TranslationResultResponse.failed();
        };
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    private Message findMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));
    }

    private void validateChatParticipation(Long userId, Message message) {
        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.MESSAGE_ALREADY_DELETED);
        }
        Long chatRoomId = message.getChatRoom().getChatRoomId();
        if (!chatPartRepository.existsByUserIdAndChatRoomId(userId, chatRoomId)) {
            throw new CustomException(ErrorCode.USER_NOT_IN_CHATROOM);
        }
    }

    private String resolvePrimaryLanguageCode(Long userId) {
        UserLanguage primary = userLanguageRepository.findByUserUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_PRIMARY_LANGUAGE_NOT_FOUND));
        return primary.getLanguage().getCode();
    }

    private TranslationResultResponse buildCompletedResponse(Media media) {
        List<Segment> segments = parseSegments(media.getSegmentsJson());
        return TranslationResultResponse.completed(
                media.getTargetLanguage(),
                media.getTranslatedText(),
                segments
        );
    }

    /** segmentsJson → Segment 리스트 변환 (VIDEO 전용, null이면 null 반환) */
    private List<Segment> parseSegments(String segmentsJson) {
        if (segmentsJson == null || segmentsJson.isBlank()) return null;
        try {
            SegmentsWrapper wrapper = SEGMENT_MAPPER.readValue(segmentsJson, SegmentsWrapper.class);
            return wrapper.segments.stream()
                    .map(s -> new Segment(
                            s.index, s.startSeconds, s.endSeconds,
                            s.timestamp, s.sourceText, s.sourceLanguage,
                            s.translatedText, s.inferenceSeconds))
                    .toList();
        } catch (Exception e) {
            return null; // 파싱 실패 시 세그먼트 없이 전문 텍스트만 반환
        }
    }

    // ── SEGMENTS_JSON 파싱용 내부 클래스 ────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SegmentsWrapper {
        List<SegmentItem> segments = List.of();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SegmentItem {
        int    index;
        double startSeconds;
        double endSeconds;
        String timestamp;
        String sourceText;
        String sourceLanguage;
        String translatedText;
        double inferenceSeconds;
    }
}
