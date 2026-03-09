package com.everybuddy.domain.friendrelation.controller;

import com.everybuddy.domain.friendrelation.service.BlockRelationService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.BlockApiSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class BlockRelationController implements BlockApiSpecification {

    private final BlockRelationService blockRelationService;

    @PostMapping("/{userId}")
    public ResponseEntity<Void> block(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long userId) {

        blockRelationService.block(userDetails.getUserId(), userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unblock(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long userId) {

        blockRelationService.unblock(userDetails.getUserId(), userId);
        return ResponseEntity.noContent().build();
    }
}
