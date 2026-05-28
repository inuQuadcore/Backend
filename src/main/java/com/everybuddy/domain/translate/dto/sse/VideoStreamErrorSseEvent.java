package com.everybuddy.domain.translate.dto.sse;

import lombok.Getter;

@Getter
public class VideoStreamErrorSseEvent {

    private final String type = "stream_error";
    private final int code;
    private final String name;
    private final String message;

    public VideoStreamErrorSseEvent(int code, String name, String message) {
        this.code = code;
        this.name = name;
        this.message = message;
    }
}
