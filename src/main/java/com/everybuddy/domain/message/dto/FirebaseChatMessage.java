package com.everybuddy.domain.message.dto;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.ZoneId;

@Getter
public class FirebaseChatMessage {
    private final Long userId;
    private final String userName;
    private final String messageType;
    private final String content;
    private final Long sendAt;

    // 파일 메시지 전용 필드 (TEXT 메시지일 때는 null로 JSON에서 제외됨)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String fileUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String fileName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Long fileSize;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String mediaType;

    @Builder
    private FirebaseChatMessage(Long userId, String userName, String messageType, String content, Long sendAt,
                                String fileUrl, String fileName, Long fileSize, String mediaType) {
        this.userId = userId;
        this.userName = userName;
        this.messageType = messageType;
        this.content = content;
        this.sendAt = sendAt;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mediaType = mediaType;
    }

    public static FirebaseChatMessage from(Message message) {
        FirebaseChatMessageBuilder builder = FirebaseChatMessage.builder()
                .userId(message.getUser().getUserId())
                .userName(message.getUser().getName())
                .messageType(message.getMessageType().name())
                .content(message.getContent())
                .sendAt(message.getSendAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        // FILE 메시지일 경우 파일 정보 추가
        if (message.getMessageType() == MessageType.FILE && message.getMedia() != null) {
            builder.fileUrl(message.getMedia().getFileKey())
                    .fileName(message.getMedia().getOriginalFilename())
                    .fileSize(message.getMedia().getFileSize())
                    .mediaType(message.getMedia().getMediaType().name());
        }

        return builder.build();
    }
}
