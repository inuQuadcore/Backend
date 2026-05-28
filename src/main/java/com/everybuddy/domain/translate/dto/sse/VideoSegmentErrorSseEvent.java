package com.everybuddy.domain.translate.dto.sse;

import lombok.Getter;

@Getter
public class VideoSegmentErrorSseEvent {

    private final String type = "segment_error";
    private final int index;
    private final int code;
    private final String name;
    private final String message;

    public VideoSegmentErrorSseEvent(int index, int code, String name, String message) {
        this.index = index;
        this.code = code;
        this.name = name;
        this.message = message;
    }
}
