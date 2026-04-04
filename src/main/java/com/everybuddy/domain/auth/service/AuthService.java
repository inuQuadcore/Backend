package com.everybuddy.domain.auth.service;

import com.everybuddy.domain.auth.dto.GoogleLoginResponse;
import com.everybuddy.domain.auth.dto.GoogleRegisterRequest;
import com.everybuddy.domain.auth.dto.LoginRequest;
import com.everybuddy.domain.auth.dto.LoginResponse;
import com.everybuddy.domain.auth.dto.RegisterRequest;
import com.everybuddy.domain.auth.entity.RefreshToken;
import com.everybuddy.domain.auth.repository.RefreshTokenRepository;
import com.everybuddy.domain.user.dto.UserLanguageRequest;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Provider;
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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.security.GeneralSecurityException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
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

    @Value("${google.client-id}")
    private String googleClientId;

    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @PostConstruct
    public void init() {
        this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public void createUser(RegisterRequest request) {
        validateDuplicateLoginId(request.getLoginId());

        User user = User.from(request, passwordEncoder);
        userRepository.save(user);

        userPresenceRepository.save(UserPresence.of(user));
        saveUserLanguages(user, request.getLanguages());
        saveUserTags(user, request.getTags());
    }

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByLoginId(loginRequest.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getProvider() != Provider.LOCAL) {
            throw new CustomException(ErrorCode.BAD_CREDENTIALS);
        }

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

    // 1. 구글 서버에 ID 토큰 검증
    // 2. ID 토큰이 유효하다면 신규 유저는 tempToken 발급, 기존 유저는 로그인(액세스/리프레쉬 토큰 발급)
    public GoogleLoginResponse authenticateWithGoogle(String idToken) {
        GoogleIdToken.Payload payload = verifyGoogleIdToken(idToken);
        return resolveGoogleUser(payload.getSubject(), payload.getEmail(), (String) payload.get("name"));
    }

    // 서버에서 발급해준 tempToken을 이용해 유효한 사용자라는 것을 증명, 회원가입 후 액세스/리프레쉬 토큰 발급
    public LoginResponse registerWithGoogle(GoogleRegisterRequest request) {
        Claims claims = jwtTokenProvider.parseTempToken(request.getTempToken());
        User user = buildOAuthUser(claims, request);
        saveOAuthUser(user, request.getTags(), request.getLanguages());
        return issueTokens(user);
    }

    private GoogleLoginResponse resolveGoogleUser(String providerId, String email, String name) {
        return userRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId)
                .map(user -> GoogleLoginResponse.existingUser(issueTokens(user)))
                .orElseGet(() -> GoogleLoginResponse.newUser(
                        jwtTokenProvider.createTempToken(providerId, Provider.GOOGLE, email, name)));
    }

    private User buildOAuthUser(Claims claims, GoogleRegisterRequest request) {
        String providerId = claims.getSubject();
        String name = claims.get("name", String.class);
        String email = claims.get("email", String.class);

        validateDuplicateLoginId(email);
        validateDuplicateOAuthUser(Provider.GOOGLE, providerId);

        return User.fromOAuth(name, email, providerId, Provider.GOOGLE, request);
    }

    private void saveOAuthUser(User user, List<String> tags, List<UserLanguageRequest> languages) {
        userRepository.save(user);
        userPresenceRepository.save(UserPresence.of(user));
        saveUserLanguages(user, languages);
        saveUserTags(user, tags);
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

    private void validateDuplicateOAuthUser(Provider provider, String providerId) {
        if (userRepository.findByProviderAndProviderId(provider, providerId).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATED_USER);
        }
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idToken) {
        try {
            GoogleIdToken token = googleIdTokenVerifier.verify(idToken);
            if (token == null) {
                throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
            }
            return token.getPayload();
        } catch (IOException | GeneralSecurityException e) {
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
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
