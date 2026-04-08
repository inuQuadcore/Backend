package com.everybuddy.domain.message.controller;

import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.message.dto.MessageResponse;
import com.everybuddy.domain.message.dto.MessageSyncResponse;
import com.everybuddy.domain.message.dto.UpdateMessageRequest;
import com.everybuddy.domain.message.service.MessageService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.MessageApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController implements MessageApiSpecification {

    private final MessageService messageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestPart("request") ChatMessageRequest request,
            @RequestPart(required = false) MultipartFile file) {

        messageService.sendMessage(userDetails.getUserId(), request, file);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageResponse> updateMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request) {

        MessageResponse response = messageService.updateMessage(userDetails.getUserId(), messageId, request);
        return ResponseEntity.ok(response);
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

    @GetMapping("/chatrooms/{chatRoomId}")
    public ResponseEntity<MessageSyncResponse> getMessages(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        MessageSyncResponse response = messageService.getMessages(userDetails.getUserId(), chatRoomId, since);
        return ResponseEntity.ok(response);
    }
}
