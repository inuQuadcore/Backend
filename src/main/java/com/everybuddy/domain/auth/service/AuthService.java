package com.everybuddy.domain.auth.service;

import com.everybuddy.domain.auth.dto.LoginRequest;
import com.everybuddy.domain.auth.dto.LoginResponse;
import com.everybuddy.domain.auth.dto.RegisterRequest;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.security.JwtTokenProvider;
import com.everybuddy.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void createUser(RegisterRequest registerRequestDto) {
        User user = User.from(registerRequestDto, passwordEncoder);
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest loginRequestDto) {
        // 사용자 조회
        String loginId = loginRequestDto.getLoginId();
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + loginId));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다");
        }

        // UserDetails 생성
        UserDetailsImpl userDetails = UserDetailsImpl.of(user);

        // Authentication 객체 생성
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        // SecurityContext에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(authentication);

        // 토큰 유효기간 (초 단위로 변환)
        Long expiresIn = jwtTokenProvider.getTokenValidityInMilliseconds() / 1000;

        return LoginResponse.of(token, expiresIn);
    }
}
