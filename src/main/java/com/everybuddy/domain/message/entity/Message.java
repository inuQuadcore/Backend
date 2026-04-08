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

    private LocalDateTime updatedAt;

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

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isEdited() {
        return this.updatedAt != null;
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

    /**
     * 테스트용 정적 팩토리 메서드
     * messageId와 sendAt을 명시적으로 설정할 수 있습니다.
     */
    public static Message createForTest(Long messageId, ChatRoom chatRoom, User user,
                                       MessageType messageType, String content, LocalDateTime sendAt) {
        Message message = create(chatRoom, user, messageType, content);
        message.messageId = messageId;
        message.sendAt = sendAt;
        return message;
    }

    /**
     * 테스트용 정적 팩토리 메서드 (Media 포함)
     * messageId와 sendAt을 명시적으로 설정할 수 있습니다.
     */
    public static Message createWithMediaForTest(Long messageId, ChatRoom chatRoom, User user,
                                                Media media, MessageType messageType, LocalDateTime sendAt) {
        Message message = createWithMedia(chatRoom, user, media, messageType);
        message.messageId = messageId;
        message.sendAt = sendAt;
        return message;
    }

    public static Message createForTestWithUpdatedAt(Long messageId, ChatRoom chatRoom, User user,
                                                     MessageType messageType, String content,
                                                     LocalDateTime sendAt, LocalDateTime updatedAt) {
        Message message = createForTest(messageId, chatRoom, user, messageType, content, sendAt);
        message.updatedAt = updatedAt;
        return message;
    }
}
