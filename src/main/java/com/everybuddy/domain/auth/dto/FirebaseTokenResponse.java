package com.everybuddy.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FirebaseTokenResponse {
    private String firebaseToken;
}
