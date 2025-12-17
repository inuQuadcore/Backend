package com.everybuddy.global.security;

import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
// TODO: ErrorCode와 JwtAuthException 클래스 생성 후 주석 해제
// import com.everybuddy.global.exception.ErrorCode;
// import com.everybuddy.global.security.exception.JwtAuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    public JwtTokenProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.tokenValidityInMilliseconds}") long tokenValidityInMilliseconds, UserRepository userRepository) {
        this.secret = secret;
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        // String 형태의 시크릿 키를 바이트 배열로 변환하여 보안 키 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    //토큰 생성
    public String createToken(Authentication authentication) {
        //토큰 만료시간 설정
        long now = new Date().getTime();
        Date validate = new Date(now + this.tokenValidityInMilliseconds);

        //토큰 생성
        return Jwts.builder()
                .subject(authentication.getName())
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
        String loginId = claims.getSubject();
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException(loginId));

        //UserDetails 생성
        UserDetailsImpl userDetails = UserDetailsImpl.of(user);

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
        } catch (JwtException e) {
            throw new RuntimeException("JWT validation failed: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("JWT token is invalid", e);
        }
    }

    public Long getTokenValidityInMilliseconds() {
        return this.tokenValidityInMilliseconds;
    }
}
