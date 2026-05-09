package com.everybuddy.domain.user.entity;

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
@Table(name = "user_language")
@EntityListeners(AuditingEntityListener.class)
public class UserLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userLanguageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private boolean isPrimary;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private UserLanguage(User user, Language language, int level, boolean isPrimary) {
        this.user = user;
        this.language = language;
        this.level = level;
        this.isPrimary = isPrimary;
    }

    public static UserLanguage of(User user, Language language, int level, boolean isPrimary) {
        return UserLanguage.builder()
                .user(user)
                .language(language)
                .level(level)
                .isPrimary(isPrimary)
                .build();
    }

    public void updateLevel(int level) {
        this.level = level;
    }
}
