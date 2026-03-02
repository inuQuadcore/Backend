package com.everybuddy.domain.user.entity;

import com.everybuddy.domain.auth.dto.RegisterRequest;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.util.EnumConverter;
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
import java.time.format.DateTimeParseException;

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

    @Column(length = 150)
    private String bio;

    @Column(nullable = false)
    private LocalDate birthday;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Builder
    private User(String loginId, String name, String password, Country country,
                 Language language, Gender gender, String profile, String bio, LocalDate birthday) {
        this.loginId = loginId;
        this.name = name;
        this.password = password;
        this.country = country;
        this.language = language;
        this.gender = gender;
        this.profile = profile;
        this.bio = bio;
        this.birthday = birthday;
    }

    public static User from(RegisterRequest registerRequest, PasswordEncoder passwordEncoder) {
        // String → Enum 변환
        Country country = EnumConverter.stringToEnum(registerRequest.getCountry(), Country.class, ErrorCode.INVALID_INPUT_VALUE);
        Gender gender = EnumConverter.stringToEnum(registerRequest.getGender(), Gender.class, ErrorCode.INVALID_INPUT_VALUE);

        // String → LocalDate 변환
        LocalDate birthday;
        try {
            birthday = LocalDate.parse(registerRequest.getBirthday());
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return User.builder()
                .loginId(registerRequest.getLoginId())
                .name(registerRequest.getName())
                .password(passwordEncoder.encode(registerRequest.getPassword()))  // 비밀번호 암호화
                .country(country)
                .language(Language.ENGLISH) // 기본값, 추후 변경 가능
                .gender(gender)
                .birthday(birthday)
                .build();
    }

    public void updateProfile(String name, LocalDate birthday, Gender gender, Country country, String bio, String profileKey) {
        if (name != null) this.name = name;
        if (birthday != null) this.birthday = birthday;
        if (gender != null) this.gender = gender;
        if (country != null) this.country = country;
        if (bio != null) this.bio = bio;
        if (profileKey != null) this.profile = profileKey;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 테스트용 정적 팩토리 메서드
     * userId를 명시적으로 설정할 수 있습니다.
     */
    public static User createForTest(Long userId, String loginId, String name, String password,
                                    Country country, Language language, Gender gender, LocalDate birthday) {
        User user = User.builder()
                .loginId(loginId)
                .name(name)
                .password(password)
                .country(country)
                .language(language)
                .gender(gender)
                .birthday(birthday)
                .build();
        user.userId = userId;
        return user;
    }

    public static User createForTest(Long userId, String loginId, String name, String password,
                                    Country country, Language language, Gender gender, LocalDate birthday, String bio) {
        User user = User.builder()
                .loginId(loginId)
                .name(name)
                .password(password)
                .country(country)
                .language(language)
                .gender(gender)
                .birthday(birthday)
                .bio(bio)
                .build();
        user.userId = userId;
        return user;
    }
}
