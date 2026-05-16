package com.everybuddy.domain.auth.service;

import com.everybuddy.domain.auth.dto.FirebaseTokenResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FirebaseTokenService {

    public FirebaseTokenResponse createFirebaseToken(Long userId) throws FirebaseAuthException {
        String uid = String.valueOf(userId);
        return FirebaseTokenResponse.of(FirebaseAuth.getInstance().createCustomToken(uid));
    }

    public void revokeTokens(Long userId) {
        try {
            FirebaseAuth.getInstance().revokeRefreshTokens(String.valueOf(userId));
        } catch (FirebaseAuthException e) {
            log.warn("Firebase 토큰 무효화 실패 - userId={}, code={}", userId, e.getAuthErrorCode());
        }
    }
}
