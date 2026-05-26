package com.everybuddy.domain.message.dto;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MessageResponse {

    private final Long messageId;
    private final Long userId;
    private final String userName;
    private final String messageType;
    private final String content;
    private final String statusPreview;
    private final LocalDateTime sendAt;
    private final LocalDateTime editedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String fileUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String fileName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Long fileSize;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String mediaType;

    /** PENDING|COMPLETED|FAILED — AUDIO·VIDEO 메시지에서만 존재 (null = 아직 번역 미요청) */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String translationStatus;

    @Builder
    private MessageResponse(Long messageId, Long userId, String userName, String messageType,
                            String content, String statusPreview, LocalDateTime sendAt, LocalDateTime editedAt,
                            String fileUrl, String fileName, Long fileSize, String mediaType,
                            String translationStatus) {
        this.messageId = messageId;
        this.userId = userId;
        this.userName = userName;
        this.messageType = messageType;
        this.content = content;
        this.statusPreview = statusPreview;
        this.sendAt = sendAt;
        this.editedAt = editedAt;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mediaType = mediaType;
        this.translationStatus = translationStatus;
    }

    private static final String DELETED_USER_NAME = "삭제된 유저";

    public static MessageResponse from(Message message, String fileUrl) {
        MessageResponseBuilder builder = MessageResponse.builder()
                .messageId(message.getMessageId())
                .userId(message.getUser().getUserId())
                .userName(message.getUser().isDeleted() ? DELETED_USER_NAME : message.getUser().getName())
                .messageType(message.getMessageType().name())
                .content(message.getContent())
                .statusPreview(message.getStatusPreview())
                .sendAt(message.getSendAt())
                .editedAt(message.getUpdatedAt());

        if (message.getMessageType() == MessageType.FILE && message.getMedia() != null) {
            var media = message.getMedia();
            builder.fileUrl(fileUrl)
                    .fileName(media.getOriginalFilename())
                    .fileSize(media.getFileSize())
                    .mediaType(media.getMediaType().name());

            // AUDIO·VIDEO 번역 상태 포함 (null이면 @JsonInclude(NON_NULL)로 응답에서 제외)
            if (media.getTranslationStatus() != null) {
                builder.translationStatus(media.getTranslationStatus().name());
            }
        }

        return builder.build();
    }
}
