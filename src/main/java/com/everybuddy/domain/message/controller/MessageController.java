package com.everybuddy.domain.message.controller;

import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.message.service.MessageService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.MessageApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController implements MessageApiSpecification {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<Void> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChatMessageRequest request) {

        messageService.sendMessage(userDetails.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long messageId) {

        messageService.deleteMessage(userDetails.getUserId(), messageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long messageId) {

        messageService.markAsRead(userDetails.getUserId(), messageId);
        return ResponseEntity.noContent().build();
    }
}
