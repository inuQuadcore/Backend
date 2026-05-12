package com.everybuddy.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Language {
    KOREAN("ko"),
    ENGLISH("en"),
    JAPANESE("ja"),
    CHINESE("zh"),
    FRENCH("fr"),
    GERMAN("de"),
    SPANISH("es"),
    RUSSIAN("ru");

    private final String code;
}
