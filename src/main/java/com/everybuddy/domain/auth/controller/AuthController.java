package com.everybuddy.domain.auth.controller;

import com.everybuddy.domain.auth.dto.RegisterRequestDto;
import com.everybuddy.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Void> createUser(RegisterRequestDto registerRequestDto){
        authService.createUser(registerRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
