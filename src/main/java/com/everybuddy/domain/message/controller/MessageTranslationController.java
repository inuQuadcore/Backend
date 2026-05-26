package com.everybuddy.domain.message.controller;

import com.everybuddy.domain.message.dto.TranslationResultResponse;
import com.everybuddy.domain.message.service.MessageTranslationService;
import com.everybuddy.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageTranslationController {

    private final MessageTranslationService translationService;

    /**
     * 번역 요청 (첫 번째 클릭 → 비동기 시작, 이미 완료 → 즉시 결과 반환).
     *
     * <pre>
     * 200 OK       → status="COMPLETED" (이미 번역된 캐시 존재)
     * 202 Accepted → status="PENDING"   (번역 시작 또는 처리 중)
     * 400          → 번역 불가 미디어 타입
     * 403          → 채팅방 미참여
     * 404          → 메시지 없음 / 삭제된 메시지
     * </pre>
     */
    @PostMapping("/{messageId}/translate")
    public ResponseEntity<TranslationResultResponse> requestTranslation(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        TranslationResultResponse response =
                translationService.requestTranslation(messageId, userDetails.getUserId());

        HttpStatus status = "COMPLETED".equals(response.getStatus())
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * 번역 결과 조회 (폴링 또는 Firebase 알림 수신 후 1회 호출).
     *
     * <pre>
     * 200 OK       → status="COMPLETED" (번역 데이터 포함)
     * 202 Accepted → status="PENDING"   (아직 처리 중)
     * 400          → 번역 불가 미디어 타입
     * 403          → 채팅방 미참여
     * 404          → 아직 번역 요청 안 됨 / 메시지 없음
     * </pre>
     */
    @GetMapping("/{messageId}/translation")
    public ResponseEntity<TranslationResultResponse> getTranslation(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        TranslationResultResponse response =
                translationService.getTranslation(messageId, userDetails.getUserId());

        HttpStatus status = "COMPLETED".equals(response.getStatus())
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;

        return ResponseEntity.status(status).body(response);
    }
}
