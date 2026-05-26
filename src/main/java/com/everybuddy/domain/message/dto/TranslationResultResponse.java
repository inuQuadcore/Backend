package com.everybuddy.domain.message.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * POST /api/v1/messages/{messageId}/translate
 * GET  /api/v1/messages/{messageId}/translation 공통 응답 DTO.
 *
 * status = "PENDING"   → translatedText·segments null
 * status = "COMPLETED" → translatedText 존재, segments는 VIDEO일 때만 존재
 * status = "FAILED"    → translatedText·segments null
 */
@Getter
@Builder
public class TranslationResultResponse {

    /** PENDING | COMPLETED | FAILED */
    private final String status;

    /** 번역 대상 언어 코드 (예: "ko") — COMPLETED 일 때만 존재 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String targetLanguage;

    /** 번역된 텍스트 전문 — COMPLETED 일 때만 존재 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String translatedText;

    /** 영상 구간별 번역 목록 — VIDEO + COMPLETED 일 때만 존재 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<Segment> segments;

    // ── 정적 팩토리 ─────────────────────────────────────────────────────────

    public static TranslationResultResponse pending() {
        return TranslationResultResponse.builder().status("PENDING").build();
    }

    public static TranslationResultResponse failed() {
        return TranslationResultResponse.builder().status("FAILED").build();
    }

    public static TranslationResultResponse completed(String targetLanguage,
                                                      String translatedText,
                                                      List<Segment> segments) {
        return TranslationResultResponse.builder()
                .status("COMPLETED")
                .targetLanguage(targetLanguage)
                .translatedText(translatedText)
                .segments(segments)
                .build();
    }

    // ── 세그먼트 (VIDEO 전용) ────────────────────────────────────────────────

    @Getter
    @AllArgsConstructor
    public static class Segment {
        private final int    index;
        private final double startSeconds;
        private final double endSeconds;
        private final String timestamp;
        private final String sourceText;
        private final String sourceLanguage;
        private final String translatedText;
        private final double inferenceSeconds;
    }
}
