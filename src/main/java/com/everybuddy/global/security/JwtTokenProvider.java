package com.everybuddy.global.security;

import com.everybuddy.domain.user.entity.Provider;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.security.exception.JwtAuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final UserRepository userRepository;
    private SecretKey key;
    private final String secret;
    private final long tokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;
    private final long tempTokenValidityInMilliseconds;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.tokenValidityInMilliseconds}") long tokenValidityInMilliseconds,
                            @Value("${jwt.refreshTokenValidityInMilliseconds}") long refreshTokenValidityInMilliseconds,
                            @Value("${jwt.tempTokenValidityInMilliseconds}") long tempTokenValidityInMilliseconds,
                            UserRepository userRepository) {
        this.secret = secret;
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
        this.tempTokenValidityInMilliseconds = tempTokenValidityInMilliseconds;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        // String 형태의 시크릿 키를 바이트 배열로 변환하여 보안 키 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    //액세스 토큰 생성
    public String createAccessToken(Long userId) {
        //토큰 만료시간 설정
        long now = new Date().getTime();
        Date validate = new Date(now + this.tokenValidityInMilliseconds);

        //토큰 생성
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .expiration(validate)
                .signWith(key)
                .compact();
    }

    //리프레쉬 토큰 생성
    public String createRefreshToken(Long userId) {
        long now = new Date().getTime();
        Date validate = new Date(now + this.refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .expiration(validate)
                .signWith(key)
                .compact();
    }

    //Jwt Token에서 사용자 정보를 추출하고 Spring Security의 Authentication 객체로 변환
    public Authentication getAuthentication(String token) {
        //토큰 파싱
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        //사용자 정보 조회
        Long userId = Long.parseLong(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JwtAuthException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new JwtAuthException(ErrorCode.USER_DELETED);
        }

        //UserDetails 생성
        UserDetailsImpl userDetails = UserDetailsImpl.create(user.getLoginId(), user.getUserId());

        return new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(this.key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            throw new JwtAuthException(ErrorCode.JWT_SIGNATURE);
            // jwt 구조가 잘못된 경우
        } catch (MalformedJwtException e) {
            throw new JwtAuthException(ErrorCode.JWT_MALFORMED);
            // 토큰 만료
        } catch (ExpiredJwtException e) {
            throw new JwtAuthException(ErrorCode.JWT_ACCESS_TOKEN_EXPIRED);
            // 서버에서 지정한 형식의 토큰이 아닌 경우 (Ex - 암호화 알고리즘이 다른 경우
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthException(ErrorCode.JWT_UNSUPPORTED);
            // 토큰이 유효하지 않은 경우 (null, 빈 문자열 등)
        } catch (IllegalArgumentException e) {
            throw new JwtAuthException(ErrorCode.JWT_NOT_VALID);
        }
    }

    public long getTokenValidityInMilliseconds() {
        return this.tokenValidityInMilliseconds;
    }

    public long getRefreshTokenValidityInMilliseconds() {
        return this.refreshTokenValidityInMilliseconds;
    }

    public String createTempToken(String providerId, Provider provider, String email, String name) {
        Date now = new Date();
        return Jwts.builder()
                .subject(providerId)
                .claim("type", "TEMP")
                .claim("provider", provider.name())
                .claim("email", email)
                .claim("name", name)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + tempTokenValidityInMilliseconds))
                .signWith(key)
                .compact();
    }

    public Claims parseTempToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"TEMP".equals(claims.get("type", String.class))) {
                throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.TEMP_TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
    }
}
