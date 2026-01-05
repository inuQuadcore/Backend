package com.everybuddy.domain.chatroom.dto;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatRoomResponse {

    private Long chatRoomId;
    private String roomName;
    private LocalDateTime createdAt;
    private List<Long> participantIds;
    private Long unreadCount;

    public static ChatRoomResponse from(ChatRoom chatRoom, List<Long> participantIds) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .roomName(chatRoom.getRoomName())
                .createdAt(chatRoom.getCreatedAt())
                .participantIds(participantIds)
                .build();
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, List<Long> participantIds, Long unreadCount) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .roomName(chatRoom.getRoomName())
                .createdAt(chatRoom.getCreatedAt())
                .participantIds(participantIds)
                .unreadCount(unreadCount)
                .build();
    }
}
