package com.everybuddy.domain.fcmtoken.service;

import com.everybuddy.domain.fcmtoken.dto.FcmTokenRegisterRequest;
import com.everybuddy.domain.fcmtoken.entity.FcmToken;
import com.everybuddy.domain.fcmtoken.repository.FcmTokenRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    public void register(Long userId, FcmTokenRegisterRequest request) {
        User user = findUser(userId);

        fcmTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        existing -> existing.updateToken(request.getToken()),
                        () -> fcmTokenRepository.save(FcmToken.of(user, request.getToken()))
                );
    }

    public void delete(Long userId) {
        User user = findUser(userId);
        fcmTokenRepository.deleteByUser(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
