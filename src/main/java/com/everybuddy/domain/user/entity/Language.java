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
    SPANISH("es"),
    FRENCH("fr"),
    GERMAN("de");

    private final String code;
}
