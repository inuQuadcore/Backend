package com.everybuddy.domain.friendrelation.controller;

import com.everybuddy.domain.friendrelation.service.FriendRelationService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.FriendApiSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendRelationController implements FriendApiSpecification {

    private final FriendRelationService friendRelationService;

    @PostMapping("/{toUserId}")
    public ResponseEntity<Void> addFriend(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long toUserId) {

        friendRelationService.addFriend(userDetails.getUserId(), toUserId);
        return ResponseEntity.ok().build();
    }
}
