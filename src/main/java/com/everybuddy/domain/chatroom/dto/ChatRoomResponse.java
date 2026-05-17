package com.everybuddy.domain.chatroom.dto;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Schema(description = "채팅방 응답")
public class ChatRoomResponse {

    @Schema(description = "채팅방 ID", example = "1")
    private final Long chatRoomId;

    @Schema(description = "채팅방 이름", example = "스터디 그룹")
    private final String roomName;

    @Schema(description = "그룹 채팅 여부 (false: 1:1, true: 그룹). 생성 시점에 결정되며 lifecycle 동안 불변.", example = "true")
    private final boolean isGroup;

    @Schema(description = "생성 시간", example = "2026-01-12T10:30:00")
    private final LocalDateTime createdAt;

    @Schema(description = "참여자 ID 목록", example = "[1, 2, 3]")
    private final List<Long> participantIds;

    @Schema(description = "읽지 않은 메시지 수", example = "5")
    private final Long unreadCount;

    @Builder
    private ChatRoomResponse(Long chatRoomId, String roomName, boolean isGroup, LocalDateTime createdAt,
                             List<Long> participantIds, Long unreadCount) {
        this.chatRoomId = chatRoomId;
        this.roomName = roomName;
        this.isGroup = isGroup;
        this.createdAt = createdAt;
        this.participantIds = participantIds;
        this.unreadCount = unreadCount;
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, List<Long> participantIds) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .roomName(chatRoom.getRoomName())
                .isGroup(chatRoom.isGroup())
                .createdAt(chatRoom.getCreatedAt())
                .participantIds(participantIds)
                .build();
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, List<Long> participantIds, Long unreadCount) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .roomName(chatRoom.getRoomName())
                .isGroup(chatRoom.isGroup())
                .createdAt(chatRoom.getCreatedAt())
                .participantIds(participantIds)
                .unreadCount(unreadCount)
                .build();
    }
}
