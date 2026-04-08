package com.everybuddy.domain.translatemessage.controller;

import com.everybuddy.domain.translatemessage.dto.TranslateSpeechResponse;
import com.everybuddy.domain.translatemessage.dto.TranslateTextRequest;
import com.everybuddy.domain.translatemessage.dto.TranslateTextResponse;
import com.everybuddy.domain.translatemessage.service.TranslateMessageService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.TranslateApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
public class TranslateMessageController implements TranslateApiSpecification {

    private final TranslateMessageService translateMessageService;

    @PostMapping("/text")
    public ResponseEntity<TranslateTextResponse> translateText(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TranslateTextRequest request) {

        TranslateTextResponse response = translateMessageService.translateText(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranslateSpeechResponse> translateSpeech(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String sourceLanguage,
            @RequestParam String targetLanguage,
            @RequestPart MultipartFile audio) {

        TranslateSpeechResponse response = translateMessageService.translateSpeech(
                userDetails.getUserId(), sourceLanguage, targetLanguage, audio);
        return ResponseEntity.ok(response);
    }
}
