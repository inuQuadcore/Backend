package com.everybuddy.domain.media.entity;

public enum TranslationStatus {
    PENDING,    // 번역 요청 후 처리 중
    COMPLETED,  // 번역 완료 (DB 캐시 유효)
    FAILED      // 번역 실패 (재시도 가능)
}
