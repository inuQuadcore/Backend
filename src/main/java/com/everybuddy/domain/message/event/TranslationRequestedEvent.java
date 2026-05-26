package com.everybuddy.domain.message.event;

import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.entity.MediaType;
import com.everybuddy.domain.message.entity.Message;
import lombok.Getter;

/**
 * 번역 요청이 처음 접수될 때(PENDING 전이 직후) 발행되는 이벤트.
 * ChatRtdbEventHandler → Firebase PENDING 업데이트
 * MessageTranslationAsyncService → 비동기 Triton 호출 시작
 */
@Getter
public class TranslationRequestedEvent {

    private final Long messageId;
    private final Long chatRoomId;
    private final Long mediaId;
    private final MediaType mediaType;

    private TranslationRequestedEvent(Long messageId, Long chatRoomId,
                                      Long mediaId, MediaType mediaType) {
        this.messageId  = messageId;
        this.chatRoomId = chatRoomId;
        this.mediaId    = mediaId;
        this.mediaType  = mediaType;
    }

    public static TranslationRequestedEvent of(Message message, Media media) {
        return new TranslationRequestedEvent(
                message.getMessageId(),
                message.getChatRoom().getChatRoomId(),
                media.getMediaId(),
                media.getMediaType()
        );
    }
}
