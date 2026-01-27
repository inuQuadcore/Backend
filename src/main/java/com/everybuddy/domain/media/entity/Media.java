package com.everybuddy.domain.media.entity;

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
@Table(name = "media")
@EntityListeners(AuditingEntityListener.class)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false, unique = true, length = 100)
    private String fileKey;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String contentType;

    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Media(User uploader, ChatRoom chatRoom, String fileKey, String originalFilename, Long fileSize, String contentType) {
        this.user = uploader;
        this.chatRoom = chatRoom;
        this.fileKey = fileKey;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public static Media from(User uploader, ChatRoom chatRoom, String fileKey, String originalFilename, Long fileSize, String contentType) {
        return Media.builder()
                .uploader(uploader)
                .chatRoom(chatRoom)
                .fileKey(fileKey)
                .originalFilename(originalFilename)
                .fileSize(fileSize)
                .contentType(contentType)
                .build();
    }

    public void softDelete(){
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted(){
        return this.deletedAt != null;
    }
}
