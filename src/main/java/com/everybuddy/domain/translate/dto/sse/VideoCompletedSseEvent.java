package com.everybuddy.domain.translate.dto.sse;

import lombok.Getter;

@Getter
public class VideoCompletedSseEvent {

    private final String type = "completed";
    private final int totalSegments;

    public VideoCompletedSseEvent(int totalSegments) {
        this.totalSegments = totalSegments;
    }
}
