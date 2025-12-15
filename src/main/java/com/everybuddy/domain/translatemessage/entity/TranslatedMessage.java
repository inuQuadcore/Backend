package com.everybuddy.domain.translatemessage.entity;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.user.entity.Language;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "translated_message")
public class TranslatedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long translatedMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @Enumerated(EnumType.STRING)
    private Language language;

    private String translatedMessage;
}
