package com.everybuddy.domain.user.controller;

import com.everybuddy.domain.user.dto.UpdateProfileRequest;
import com.everybuddy.domain.user.dto.UserProfileResponse;
import com.everybuddy.domain.user.service.UserService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.UserApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserApiSpecification {

    private final UserService userService;

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
