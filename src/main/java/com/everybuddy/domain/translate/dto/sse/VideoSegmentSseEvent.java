package com.everybuddy.domain.translate.dto.sse;

import lombok.Getter;

@Getter
public class VideoSegmentSseEvent {

    private final String type = "segment_result";
    private final int index;
    private final double startSeconds;
    private final double endSeconds;
    private final String timestamp;
    private final String sourceText;
    private final String sourceLanguage;
    private final String translatedText;
    private final double inferenceSeconds;

    public VideoSegmentSseEvent(int index, double startSeconds, double endSeconds,
                                String timestamp, String sourceText, String sourceLanguage,
                                String translatedText, double inferenceSeconds) {
        this.index = index;
        this.startSeconds = startSeconds;
        this.endSeconds = endSeconds;
        this.timestamp = timestamp;
        this.sourceText = sourceText;
        this.sourceLanguage = sourceLanguage;
        this.translatedText = translatedText;
        this.inferenceSeconds = inferenceSeconds;
    }
}
