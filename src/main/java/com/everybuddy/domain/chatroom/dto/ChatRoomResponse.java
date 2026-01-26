package com.everybuddy.domain.chatroom.dto;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "채팅방 응답")
public class ChatRoomResponse {

    @Schema(description = "채팅방 ID", example = "1")
    private Long chatRoomId;

    @Schema(description = "채팅방 이름", example = "스터디 그룹")
    private String roomName;

    @Schema(description = "생성 시간", example = "2026-01-12T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "참여자 ID 목록", example = "[1, 2, 3]")
    private List<Long> participantIds;

    @Schema(description = "읽지 않은 메시지 수", example = "5")
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
