package com.everybuddy.domain.message.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class MessageSyncResponse {

    private final List<MessageResponse> newMessages;
    private final List<MessageResponse> updatedMessages;
    private final List<Long> deletedIds;

    @Builder
    private MessageSyncResponse(List<MessageResponse> newMessages,
                                List<MessageResponse> updatedMessages,
                                List<Long> deletedIds) {
        this.newMessages = newMessages;
        this.updatedMessages = updatedMessages;
        this.deletedIds = deletedIds;
    }

    public static MessageSyncResponse of(List<MessageResponse> newMessages,
                                         List<MessageResponse> updatedMessages,
                                         List<Long> deletedIds) {
        return builder()
                .newMessages(newMessages)
                .updatedMessages(updatedMessages)
                .deletedIds(deletedIds)
                .build();
    }
}
