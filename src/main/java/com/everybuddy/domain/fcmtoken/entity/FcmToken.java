package com.everybuddy.domain.fcmtoken.entity;

import com.everybuddy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fcm_token")
@EntityListeners(AuditingEntityListener.class)
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fcmTokenId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private FcmToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public static FcmToken of(User user, String token) {
        return FcmToken.builder()
                .user(user)
                .token(token)
                .build();
    }

    public static FcmToken createForTest(Long fcmTokenId, User user, String token) {
        FcmToken fcmToken = FcmToken.builder()
                .user(user)
                .token(token)
                .build();
        fcmToken.fcmTokenId = fcmTokenId;
        return fcmToken;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
