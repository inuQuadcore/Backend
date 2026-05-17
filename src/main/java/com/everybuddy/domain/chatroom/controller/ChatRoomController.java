package com.everybuddy.domain.chatroom.controller;

import com.everybuddy.domain.chatroom.dto.ChatRoomResponse;
import com.everybuddy.domain.chatroom.dto.CreateChatRoomRequest;
import com.everybuddy.domain.chatroom.dto.InviteMembersRequest;
import com.everybuddy.domain.chatroom.service.ChatRoomService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.ChatRoomApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController implements ChatRoomApiSpecification {

    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateChatRoomRequest request) {

        ChatRoomResponse response = chatRoomService.createChatRoom(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyChatRooms(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<ChatRoomResponse> response = chatRoomService.getMyChatRooms(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chatRoomId}/participants/me")
    public ResponseEntity<Void> leaveChatRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long chatRoomId) {

        chatRoomService.leaveChatRoom(userDetails.getUserId(), chatRoomId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chatRoomId}/participants")
    public ResponseEntity<Void> inviteMembers(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody InviteMembersRequest request) {

        chatRoomService.inviteMembers(userDetails.getUserId(), chatRoomId, request);
        return ResponseEntity.noContent().build();
    }

}
