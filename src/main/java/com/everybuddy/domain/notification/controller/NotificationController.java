package com.everybuddy.domain.notification.controller;

import com.everybuddy.domain.notification.dto.HasUnreadResponse;
import com.everybuddy.domain.notification.dto.NotificationListResponse;
import com.everybuddy.domain.notification.service.NotificationService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.NotificationApiSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationApiSpecification {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> getList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(notificationService.getList(userDetails.getUserId(), before, limit));
    }

    @GetMapping("/has-unread")
    public ResponseEntity<HasUnreadResponse> hasUnread(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        return ResponseEntity.ok(notificationService.hasUnread(userDetails.getUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long notificationId) {

        notificationService.markRead(userDetails.getUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        notificationService.markAllRead(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
