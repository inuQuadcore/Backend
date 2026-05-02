package com.everybuddy.domain.translate.controller;

import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.domain.translate.service.TranslateService;
import com.everybuddy.global.swagger.TranslateApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
public class TranslateController implements TranslateApiSpecification {

    private final TranslateService translateService;

    @PostMapping("/text")
    public ResponseEntity<TextTranslateResponse> translateText(
            @Valid @RequestBody TextTranslateRequest request) {

        TextTranslateResponse response = translateService.translateText(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpeechTranslateResponse> translateSpeech(
            @RequestPart MultipartFile file,
            @RequestParam String targetLang) {

        SpeechTranslateResponse response = translateService.translateSpeech(file, targetLang);
        return ResponseEntity.ok(response);
    }
}
