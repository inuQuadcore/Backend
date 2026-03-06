package com.everybuddy.domain.chatroom.event;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatRoomCreatedEvent {
    private final Long chatRoomId;
    private final List<Long> participantIds;

    @Builder
    private ChatRoomCreatedEvent(Long chatRoomId, List<Long> participantIds) {
        this.chatRoomId = chatRoomId;
        this.participantIds = participantIds;
    }

    public static ChatRoomCreatedEvent of(Long chatRoomId, List<Long> participantIds) {
        return builder()
                .chatRoomId(chatRoomId)
                .participantIds(participantIds)
                .build();
    }
}
