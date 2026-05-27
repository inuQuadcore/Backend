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

}

