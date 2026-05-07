package com.everybuddy.global.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Schema(name = "SpeechTranslateMultipart", description = "음성 번역 멀티파트 요청")
public class SpeechTranslateMultipart {

    @Schema(
            description = """
                    번역할 오디오 파일 (최대 50MB)
                    - 지원 형식: wav, mp3, ogg, webm, mp4, m4a
                    """,
            type = "string",
            format = "binary"
    )
    private MultipartFile file;

    @Schema(
            description = "번역 목표 언어 (KOREAN, ENGLISH, JAPANESE, CHINESE, SPANISH, FRENCH, GERMAN)",
            allowableValues = {"KOREAN", "ENGLISH", "JAPANESE", "CHINESE", "SPANISH", "FRENCH", "GERMAN"},
            example = "KOREAN"
    )
    private String targetLang;
}
