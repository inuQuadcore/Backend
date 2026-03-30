package com.everybuddy.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_presence")
public class UserPresence {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_online", nullable = false)
    private boolean isOnline;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Builder
    private UserPresence(User user) {
        this.user = user;
        this.isOnline = false;
        this.lastSeenAt = null;
    }

    public static UserPresence of(User user) {
        return UserPresence.builder()
                .user(user)
                .build();
    }

}
