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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Builder
    private ChatRoom(String roomName) {
        this.roomName = roomName;
    }

    public static ChatRoom create(String roomName) {
        return ChatRoom.builder()
                .roomName(roomName)
                .build();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 테스트용 정적 팩토리 메서드
     * chatRoomId를 명시적으로 설정할 수 있습니다.
     */
    public static ChatRoom createForTest(Long chatRoomId, String roomName) {
        ChatRoom chatRoom = ChatRoom.create(roomName);
        chatRoom.chatRoomId = chatRoomId;
        return chatRoom;
    }
}
