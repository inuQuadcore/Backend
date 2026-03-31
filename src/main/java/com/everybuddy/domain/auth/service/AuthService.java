package com.everybuddy.domain.auth.service;

import com.everybuddy.domain.auth.dto.LoginRequest;
import com.everybuddy.domain.auth.dto.LoginResponse;
import com.everybuddy.domain.auth.dto.RegisterRequest;
import com.everybuddy.domain.auth.entity.RefreshToken;
import com.everybuddy.domain.auth.repository.RefreshTokenRepository;
import com.everybuddy.domain.user.dto.UserLanguageRequest;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Tag;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserPresence;
import com.everybuddy.domain.user.entity.UserTag;
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.domain.user.repository.UserPresenceRepository;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.domain.user.repository.UserTagRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.security.JwtTokenProvider;
import com.everybuddy.global.util.EnumConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserPresenceRepository userPresenceRepository;
    private final UserLanguageRepository userLanguageRepository;
    private final UserTagRepository userTagRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public void createUser(RegisterRequest request) {
        validateDuplicateLoginId(request.getLoginId());

        User user = User.from(request, passwordEncoder);
        userRepository.save(user);

        userPresenceRepository.save(UserPresence.of(user));
        saveUserLanguages(user, request.getLanguages());
        saveUserTags(user, request.getTags());
    }

    public LoginResponse login(LoginRequest loginRequest) {
        String loginId = loginRequest.getLoginId();
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.BAD_CREDENTIALS);
        }

        return issueTokens(user);
    }

    public LoginResponse refresh(String refreshTokenStr) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (stored.isExpired()) {
            refreshTokenRepository.deleteByUser(stored.getUser());
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = stored.getUser();
        refreshTokenRepository.deleteByUser(user);
        return issueTokens(user);
    }

    public void logout(String refreshTokenStr) {
        refreshTokenRepository.deleteByToken(refreshTokenStr);
    }

    private LoginResponse issueTokens(User user) {
        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        LocalDateTime accessTokenExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getTokenValidityInMilliseconds() / 1000);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

        refreshTokenRepository.save(RefreshToken.of(user, refreshToken, refreshTokenExpiresAt));

        return LoginResponse.of(user.getUserId(), accessToken, accessTokenExpiresAt,
                refreshToken, refreshTokenExpiresAt);
    }

    private void validateDuplicateLoginId(String loginId) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new CustomException(ErrorCode.DUPLICATED_USER);
        }
    }

    private void saveUserLanguages(User user, List<UserLanguageRequest> languages) {
        List<UserLanguage> userLanguages = languages.stream()
                .distinct()
                .map(lr -> {
                    Language language = EnumConverter.stringToEnum(lr.getLanguage(), Language.class, ErrorCode.INVALID_INPUT_VALUE);
                    return UserLanguage.of(user, language, lr.getLevel());
                })
                .toList();
        userLanguageRepository.saveAll(userLanguages);
    }

    private void saveUserTags(User user, List<String> tags) {
        List<UserTag> userTags = tags.stream()
                .distinct()
                .map(tag -> EnumConverter.stringToEnum(tag, Tag.class, ErrorCode.INVALID_INPUT_VALUE))
                .map(tag -> UserTag.of(user, tag))
                .toList();
        userTagRepository.saveAll(userTags);
    }
}
