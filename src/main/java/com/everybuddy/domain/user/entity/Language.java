package com.everybuddy.domain.user.entity;

import java.util.Optional;

public enum Language {
    KOREAN("ko"),
    ENGLISH("en"),
    JAPANESE("ja"),
    CHINESE("zh"),
    SPANISH("es"),
    FRENCH("fr"),
    GERMAN("de");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<Language> fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return Optional.of(lang);
            }
        }
        return Optional.empty();
    }
}
