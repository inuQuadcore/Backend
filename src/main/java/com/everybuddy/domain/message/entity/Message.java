package com.everybuddy.domain.message.entity;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.media.entity.Media;
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

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "media_id")
    private Media media;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    private String content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime sendAt;

    private LocalDateTime deletedAt;

    @Builder
    private Message(ChatRoom chatRoom, User user, Media media, MessageType messageType, String content) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.media = media;
        this.messageType = messageType;
        this.content = content;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public static Message create(ChatRoom chatRoom, User user, MessageType messageType, String content) {
        return Message.builder()
                .chatRoom(chatRoom)
                .user(user)
                .messageType(messageType)
                .content(content)
                .build();
    }

    public static Message createWithMedia(ChatRoom chatRoom, User user, Media media, MessageType messageType) {
        return Message.builder()
                .chatRoom(chatRoom)
                .user(user)
                .media(media)
                .messageType(messageType)
                .content(null)
                .build();
    }
}
