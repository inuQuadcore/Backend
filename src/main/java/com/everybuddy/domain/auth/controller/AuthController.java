package com.everybuddy.domain.auth.controller;

import com.everybuddy.domain.auth.dto.FirebaseTokenResponse;
import com.everybuddy.domain.auth.dto.LoginRequest;
import com.everybuddy.domain.auth.dto.LoginResponse;
import com.everybuddy.domain.auth.dto.RegisterRequest;
import com.everybuddy.domain.auth.service.AuthService;
import com.everybuddy.domain.auth.service.FirebaseTokenService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.google.firebase.auth.FirebaseAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final FirebaseTokenService firebaseTokenService;

    @PostMapping("/register")
    public ResponseEntity<Void> createUser(@RequestBody RegisterRequest registerRequestDto){
        authService.createUser(registerRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequestDto){
        LoginResponse loginResponseDto = authService.login(loginRequestDto);
        return ResponseEntity.ok(loginResponseDto);
    }

    @GetMapping("/firebase-token")
    public ResponseEntity<FirebaseTokenResponse> getFirebaseToken(
            @AuthenticationPrincipal UserDetailsImpl userDetails) throws FirebaseAuthException {

        FirebaseTokenResponse firebaseToken = firebaseTokenService.createFirebaseToken(userDetails.getUserId());
        return ResponseEntity.ok(firebaseToken);
    }
}
