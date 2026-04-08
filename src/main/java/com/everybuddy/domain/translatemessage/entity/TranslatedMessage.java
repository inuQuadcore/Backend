package com.everybuddy.domain.translatemessage.entity;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.user.entity.Language;
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
@Table(name = "translated_message")
@EntityListeners(AuditingEntityListener.class)
public class TranslatedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long translatedMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language sourceLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language targetLanguage;

    @Column(columnDefinition = "TEXT")
    private String originalText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translatedMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranslationType type;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private TranslatedMessage(User user, Message message, Language sourceLanguage,
                               Language targetLanguage, String originalText,
                               String translatedMessage, TranslationType type) {
        this.user = user;
        this.message = message;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.originalText = originalText;
        this.translatedMessage = translatedMessage;
        this.type = type;
    }

    public static TranslatedMessage of(User user, Language sourceLanguage, Language targetLanguage,
                                       String originalText, String translatedMessage, TranslationType type) {
        return TranslatedMessage.builder()
                .user(user)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .originalText(originalText)
                .translatedMessage(translatedMessage)
                .type(type)
                .build();
    }
}
