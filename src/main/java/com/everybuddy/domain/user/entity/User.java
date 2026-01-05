package com.everybuddy.domain.user.entity;

import com.everybuddy.domain.auth.dto.RegisterRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user")
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Country country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    private String profile;

    private String hobby;

    @Column(nullable = false)
    private LocalDate birthday;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Builder
    private User(String loginId, String name, String password, Country country,
                 Language language, Gender gender, String profile, String hobby, LocalDate birthday) {
        this.loginId = loginId;
        this.name = name;
        this.password = password;
        this.country = country;
        this.language = language;
        this.gender = gender;
        this.profile = profile;
        this.hobby = hobby;
        this.birthday = birthday;
    }

    public static User from(RegisterRequest registerRequest, PasswordEncoder passwordEncoder) {
        return User.builder()
                .loginId(registerRequest.getLoginId())
                .name(registerRequest.getName())
                .password(passwordEncoder.encode(registerRequest.getPassword()))  // 비밀번호 암호화
                .country(registerRequest.getCountry())
                .language(Language.ENGLISH) // 기본값, 추후 변경 가능
                .gender(registerRequest.getGender())
                .birthday(registerRequest.getBirthday())
                .build();
    }
}
