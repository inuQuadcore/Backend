package com.everybuddy.domain.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
public class VideoTranslateResponse {

    private final String translatedText;
    private final List<Segment> segments;

    private VideoTranslateResponse(String translatedText, List<Segment> segments) {
        this.translatedText = translatedText;
        this.segments = segments;
    }

    public static VideoTranslateResponse of(String translatedText, List<Segment> segments) {
        return new VideoTranslateResponse(
                translatedText != null ? translatedText : "",
                segments != null ? segments : List.of()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class Segment {
        private int index;
        private double startSeconds;
        private double endSeconds;
        private String timestamp;
        private String sourceText;
        private String sourceLanguage;
        private String translatedText;
        private double inferenceSeconds;
    }
}
