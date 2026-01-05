package com.everybuddy.domain.message.entity;

import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.user.entity.User;
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
@Table(name = "message")
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    private String content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime sendAt;

    @Builder
    private Message(ChatRoom chatRoom, User user, MessageType messageType, String content) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.messageType = messageType;
        this.content = content;
    }

    public static Message create(ChatRoom chatRoom, User user, ChatMessageRequest chatMessageRequest) {
        return Message.builder()
                .chatRoom(chatRoom)
                .user(user)
                .messageType(chatMessageRequest.getMessageType())
                .content(chatMessageRequest.getContent())
                .build();
    }
}
