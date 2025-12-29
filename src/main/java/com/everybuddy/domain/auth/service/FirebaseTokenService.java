package com.everybuddy.domain.auth.service;

import com.everybuddy.domain.auth.dto.FirebaseTokenResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

@Service
public class FirebaseTokenService {

    public FirebaseTokenResponse createFirebaseToken(Long userId) throws FirebaseAuthException {
        String uid = String.valueOf(userId);
        return new FirebaseTokenResponse(FirebaseAuth.getInstance().createCustomToken(uid));
    }
}
