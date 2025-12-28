package com.everybuddy.domain.message.controller;

import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.message.dto.ChatMessageResponse;
import com.everybuddy.domain.message.service.MessageService;
import com.everybuddy.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody ChatMessageRequest request) {

        ChatMessageResponse response = messageService.sendMessage(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
