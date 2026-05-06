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
        @Index(name = "idx_notification_to_user_id", columnList = "to_user_id, notification_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    @Column(nullable = false, length = 200)
    private String body;

    private LocalDateTime readAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Notification(User toUser, User fromUser, String body) {
        this.toUser = toUser;
        this.fromUser = fromUser;
        this.body = body;
    }

    public static Notification of(User toUser, User fromUser, String body) {
        return Notification.builder()
                .toUser(toUser)
                .fromUser(fromUser)
                .body(body)
                .build();
    }

    public static Notification createForTest(Long notificationId, User toUser, User fromUser, String body,
                                             LocalDateTime createdAt, LocalDateTime readAt) {
        Notification notification = Notification.builder()
                .toUser(toUser)
                .fromUser(fromUser)
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
        return this.toUser.getUserId().equals(userId);
    }
}
