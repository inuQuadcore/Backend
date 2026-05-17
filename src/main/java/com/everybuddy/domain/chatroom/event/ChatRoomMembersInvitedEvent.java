package com.everybuddy.domain.chatroom.event;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatRoomMembersInvitedEvent {
    private final Long chatRoomId;
    private final List<Long> invitedUserIds;

    @Builder
    private ChatRoomMembersInvitedEvent(Long chatRoomId, List<Long> invitedUserIds) {
        this.chatRoomId = chatRoomId;
        this.invitedUserIds = invitedUserIds;
    }

    public static ChatRoomMembersInvitedEvent of(Long chatRoomId, List<Long> invitedUserIds) {
        return builder()
                .chatRoomId(chatRoomId)
                .invitedUserIds(invitedUserIds)
                .build();
    }
}
