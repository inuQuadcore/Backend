package com.everybuddy.global.swagger;

import com.everybuddy.domain.message.dto.ChatMessageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Schema(name = "SendMessageMultipart", description = "메시지 전송 멀티파트 요청")
public class SendMessageMultipart {

    @Schema(description = "메시지 전송 요청(JSON)", implementation = ChatMessageRequest.class, required = true)
    private ChatMessageRequest request;

    @Schema(description = """
            전송할 파일 (선택적, 파일 포함 시 파일 메시지로 처리)
            - 최대 크기: 10MB
            - 이미지: jpg, jpeg, png, gif, webp, heic
            - 비디오: mp4, mov, avi, webm
            - 오디오: mp3, wav, m4a, aac
            - 문서: pdf, txt, doc, docx, xls, xlsx, ppt, pptx
            - 압축: zip, rar
            """, type = "string", format = "binary")
    private MultipartFile file;
}
