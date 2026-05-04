package com.everybuddy.domain.notification.entity;

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
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_recipient_id", columnList = "recipient_id, notification_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 200)
    private String body;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private LocalDateTime readAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Notification(User recipient, NotificationType type, String title, String body, String payload) {
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.body = body;
        this.payload = payload;
    }

    public static Notification of(User recipient, NotificationType type, String title, String body, String payload) {
        return Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .payload(payload)
                .build();
    }

    public static Notification createForTest(Long notificationId, User recipient, NotificationType type,
                                             String title, String body, LocalDateTime createdAt, LocalDateTime readAt) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .build();
        notification.notificationId = notificationId;
        notification.createdAt = createdAt;
        notification.readAt = readAt;
        return notification;
    }

    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isRead() {
        return this.readAt != null;
    }

    public boolean isOwnedBy(Long userId) {
        return this.recipient.getUserId().equals(userId);
    }
}
