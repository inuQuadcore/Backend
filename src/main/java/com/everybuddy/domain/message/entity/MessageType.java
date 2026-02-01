package com.everybuddy.domain.message.entity;

public enum MessageType {
    TEXT,  // 텍스트 메시지
    FILE   // 파일 첨부 메시지 (세부 타입은 Media.mediaType으로 구분)
}
