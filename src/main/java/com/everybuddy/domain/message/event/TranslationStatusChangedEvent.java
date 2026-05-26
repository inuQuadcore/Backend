package com.everybuddy.domain.message.event;

import com.everybuddy.domain.media.entity.TranslationStatus;
import lombok.Getter;

/**
 * 비동기 번역이 완료(COMPLETED) 또는 실패(FAILED)로 전이될 때 발행되는 이벤트.
 * ChatRtdbEventHandler → Firebase 상태 업데이트
 */
@Getter
public class TranslationStatusChangedEvent {

    private final Long messageId;
    private final Long chatRoomId;
    private final TranslationStatus status;

    private TranslationStatusChangedEvent(Long messageId, Long chatRoomId, TranslationStatus status) {
        this.messageId  = messageId;
        this.chatRoomId = chatRoomId;
        this.status     = status;
    }

    public static TranslationStatusChangedEvent of(Long messageId, Long chatRoomId, TranslationStatus status) {
        return new TranslationStatusChangedEvent(messageId, chatRoomId, status);
    }
}
