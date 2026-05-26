package com.everybuddy.domain.translate.controller;

import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.domain.translate.dto.TtsRequest;
import com.everybuddy.domain.translate.dto.VideoTranslateResponse;
import com.everybuddy.domain.translate.service.TranslateService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.TranslateApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import reactor.core.Disposable;

@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
public class TranslateController implements TranslateApiSpecification {

    private final TranslateService translateService;

    @PostMapping("/text")
    public ResponseEntity<TextTranslateResponse> translateText(
            @Valid @RequestBody TextTranslateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        TextTranslateResponse response = translateService.translateText(request, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpeechTranslateResponse> translateSpeech(
            @RequestPart MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        SpeechTranslateResponse response = translateService.translateSpeech(file, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tts")
    public ResponseEntity<byte[]> tts(
            @Valid @RequestBody TtsRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        byte[] audioBytes = translateService.tts(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        headers.setContentDisposition(ContentDisposition.inline().filename("tts_output.wav").build());

        return ResponseEntity.ok().headers(headers).body(audioBytes);
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoTranslateResponse> translateVideo(
            @RequestPart MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        VideoTranslateResponse response = translateService.translateVideo(file, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 영상 번역 스트리밍 (SSE).
     * 구간(segment)이 처리될 때마다 즉시 클라이언트로 전송.
     * Triton gemma_s2tt_stream (Decoupled) 모델 사용.
     * 프론트엔드는 fetch() + ReadableStream으로 수신 (EventSource API는 POST 미지원).
     */
    @PostMapping(value = "/video/stream",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter translateVideoStream(
            @RequestPart MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

        Disposable subscription = translateService.translateVideoStream(file, userDetails.getUserId())
                .subscribe(
                        json -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("segment")
                                        .data(json, MediaType.APPLICATION_JSON));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );

        // 클라이언트 연결 종료 시 Triton 스트리밍 취소
        emitter.onTimeout(subscription::dispose);
        emitter.onCompletion(subscription::dispose);

        return emitter;
    }
}
