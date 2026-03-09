package com.everybuddy.domain.statusmessage.controller;

import com.everybuddy.domain.statusmessage.dto.CreateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.service.StatusMessageService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.StatusMessageApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status-messages")
@RequiredArgsConstructor
public class StatusMessageController implements StatusMessageApiSpecification {

    private final StatusMessageService statusMessageService;

    @PostMapping
    public ResponseEntity<Void> createStatusMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateStatusMessageRequest request) {

        statusMessageService.createStatusMessage(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }
}
