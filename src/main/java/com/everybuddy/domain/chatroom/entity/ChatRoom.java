package com.everybuddy.domain.chatroom.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chatroom")
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomId;

    @Column(nullable = false)
    private String roomName;

    @Column(nullable = false)
    private boolean isGroup;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Builder
    private ChatRoom(String roomName, boolean isGroup) {
        this.roomName = roomName;
        this.isGroup = isGroup;
    }

    public static ChatRoom create(String roomName, boolean isGroup) {
        return ChatRoom.builder()
                .roomName(roomName)
                .isGroup(isGroup)
                .build();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public static ChatRoom createForTest(Long chatRoomId, String roomName, boolean isGroup) {
        ChatRoom chatRoom = ChatRoom.create(roomName, isGroup);
        chatRoom.chatRoomId = chatRoomId;
        return chatRoom;
    }
}
