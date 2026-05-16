package com.everybuddy.domain.user.controller;

import com.everybuddy.domain.user.dto.UpdateProfileRequest;
import com.everybuddy.domain.user.dto.UpdateTagsRequest;
import com.everybuddy.domain.user.dto.UserLanguageRequest;
import com.everybuddy.domain.user.dto.UserLanguagesResponse;
import com.everybuddy.domain.user.dto.UserProfileResponse;
import com.everybuddy.domain.user.dto.UserProfileViewResponse;
import com.everybuddy.domain.user.dto.UserTagResponse;
import com.everybuddy.domain.user.service.UserService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.UserApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserApiSpecification {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileViewResponse> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        UserProfileViewResponse response = userService.getUserProfile(userId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/tags")
    public ResponseEntity<List<UserTagResponse>> getUserTags(
            @PathVariable Long userId) {

        List<UserTagResponse> response = userService.getUserTags(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/languages")
    public ResponseEntity<UserLanguagesResponse> getUserLanguages(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        UserLanguagesResponse response = userService.getUserLanguages(userId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/languages")
    public ResponseEntity<Void> updateLanguageLevel(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UserLanguageRequest request) {

        userService.updateLanguageLevel(userDetails.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/tags")
    public ResponseEntity<Void> updateTags(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateTagsRequest request) {

        userService.updateTags(userDetails.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        userService.deleteUser(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestPart("request") UpdateProfileRequest request,
            @RequestPart(required = false) MultipartFile profileImage) {

        UserProfileResponse response = userService.updateProfile(
                userDetails.getUserId(), request, profileImage);
        return ResponseEntity.ok(response);
    }
}
