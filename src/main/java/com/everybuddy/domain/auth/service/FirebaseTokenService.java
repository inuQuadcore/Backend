package com.everybuddy.domain.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

@Service
public class FirebaseTokenService {

    public String createFirebaseToken(Long userId) throws FirebaseAuthException {
        String uid = String.valueOf(userId);
        return FirebaseAuth.getInstance().createCustomToken(uid);
    }
}
