package com.everybuddy.domain.fcmtoken.controller;

import com.everybuddy.domain.fcmtoken.dto.FcmTokenRegisterRequest;
import com.everybuddy.domain.fcmtoken.service.FcmTokenService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.FcmTokenApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fcm-tokens")
@RequiredArgsConstructor
public class FcmTokenController implements FcmTokenApiSpecification {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody FcmTokenRegisterRequest request) {

        fcmTokenService.register(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        fcmTokenService.delete(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
