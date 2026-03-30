package com.everybuddy.domain.discover.controller;

import com.everybuddy.domain.discover.dto.FilterDiscoverRequest;
import com.everybuddy.domain.discover.dto.FilterDiscoverResponse;
import com.everybuddy.domain.discover.dto.RandomDiscoverResponse;
import com.everybuddy.domain.discover.service.DiscoverService;
import com.everybuddy.global.security.UserDetailsImpl;
import com.everybuddy.global.swagger.DiscoverApiSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discover")
@RequiredArgsConstructor
public class DiscoverController implements DiscoverApiSpecification {

    private final DiscoverService discoverService;

    @GetMapping("/random")
    public ResponseEntity<RandomDiscoverResponse> getRandomUsers(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        return ResponseEntity.ok(discoverService.getRandomUsers(userDetails.getUserId()));
    }

    @GetMapping("/filter")
    public ResponseEntity<FilterDiscoverResponse> getFilteredUsers(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute FilterDiscoverRequest request) {

        return ResponseEntity.ok(discoverService.getFilteredUsers(userDetails.getUserId(), request));
    }
}
