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

    @Schema(description = "참여자 정보 (본인 포함). 클라가 1:1방 상대 이름·그룹방 아바타 표시에 활용.")
    private final List<ChatRoomParticipantResponse> participants;

    @Schema(description = "읽지 않은 메시지 수", example = "5")
    private final Long unreadCount;

    @Schema(description = "마지막 메시지 프리뷰 (본인 입장 이후 메시지 기준). 없으면 null. 파일 메시지는 '사진을 보냈습니다.' 등으로 치환. 삭제된 메시지면 '삭제된 메시지입니다'.",
            example = "안녕하세요")
    private final String lastMessage;

    @Schema(description = "마지막 메시지 전송 시각 (없으면 null)", example = "2026-05-17T20:00:00")
    private final LocalDateTime lastMessageTime;

    @Builder
    private ChatRoomResponse(Long chatRoomId, String roomName, boolean isGroup, LocalDateTime createdAt,
                             List<ChatRoomParticipantResponse> participants, Long unreadCount,
                             String lastMessage, LocalDateTime lastMessageTime) {
        this.chatRoomId = chatRoomId;
        this.roomName = roomName;
        this.isGroup = isGroup;
        this.createdAt = createdAt;
        this.participants = participants;
        this.unreadCount = unreadCount;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, List<ChatRoomParticipantResponse> participants) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .roomName(chatRoom.getRoomName())
                .isGroup(chatRoom.isGroup())
                .createdAt(chatRoom.getCreatedAt())
                .participants(participants)
                .build();
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, List<ChatRoomParticipantResponse> participants,
                                        Long unreadCount, String lastMessage, LocalDateTime lastMessageTime) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .roomName(chatRoom.getRoomName())
                .isGroup(chatRoom.isGroup())
                .createdAt(chatRoom.getCreatedAt())
                .participants(participants)
                .unreadCount(unreadCount)
                .lastMessage(lastMessage)
                .lastMessageTime(lastMessageTime)
                .build();
    }
}
