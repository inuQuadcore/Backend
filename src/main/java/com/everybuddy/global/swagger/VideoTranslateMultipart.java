package com.everybuddy.global.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Schema(name = "VideoTranslateMultipart", description = "영상 번역 멀티파트 요청")
public class VideoTranslateMultipart {

    @Schema(
            description = """
                    번역할 영상 파일 (최대 50MB)
                    - 지원 형식: mp4, mov
                    """,
            type = "string",
            format = "binary"
    )
    private MultipartFile file;
}
