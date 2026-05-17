package com.everybuddy.domain.user.entity;

import com.everybuddy.domain.auth.dto.GoogleRegisterRequest;
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
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user")
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true)
    private String loginId;

    @Column(nullable = false)
    private String name;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'LOCAL'")
    private Provider provider;

    @Column
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Country country;

    private String profile;

    @Column(length = 150)
    private String bio;

    @Column(nullable = false)
    private LocalDate birthday;

    private LocalDate lastAttendanceDate;

    @Column(nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private int consecutiveDays;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Builder
    private User(String loginId, String name, String password, Country country,
                 Gender gender, String profile, String bio, LocalDate birthday,
                 Provider provider, String providerId) {
        this.loginId = loginId;
        this.name = name;
        this.password = password;
        this.country = country;
        this.gender = gender;
        this.profile = profile;
        this.bio = bio;
        this.birthday = birthday;
        this.provider = provider;
        this.providerId = providerId;
    }

    public static User from(RegisterRequest registerRequest, PasswordEncoder passwordEncoder) {
        return User.builder()
                .loginId(registerRequest.getLoginId())
                .name(registerRequest.getName())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .country(parseCountry(registerRequest.getCountry()))
                .gender(parseGender(registerRequest.getGender()))
                .bio(registerRequest.getBio())
                .birthday(parseBirthday(registerRequest.getBirthday()))
                .provider(Provider.LOCAL)
                .build();
    }

    public static User fromOAuth(String name, String email, String providerId, Provider provider, GoogleRegisterRequest request) {
        return User.builder()
                .loginId(email)
                .name(name)
                .country(parseCountry(request.getCountry()))
                .gender(parseGender(request.getGender()))
                .bio(request.getBio())
                .birthday(parseBirthday(request.getBirthday()))
                .provider(provider)
                .providerId(providerId)
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
        this.loginId = "deleted_" + UUID.randomUUID();
        if (this.providerId != null) {
            this.providerId = "deleted_" + UUID.randomUUID();
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void recordAttendance(LocalDate today) {
        if (today.equals(lastAttendanceDate)) {
            return;
        }
        if (lastAttendanceDate != null && today.minusDays(1).equals(lastAttendanceDate)) {
            this.consecutiveDays = this.consecutiveDays + 1;
        } else {
            this.consecutiveDays = 1;
        }
        this.lastAttendanceDate = today;
    }

    public int getCurrentConsecutiveDays(LocalDate today) {
        if (lastAttendanceDate == null) {
            return 0;
        }
        if (lastAttendanceDate.equals(today) || lastAttendanceDate.equals(today.minusDays(1))) {
            return consecutiveDays;
        }
        return 0;
    }

    /**
     * 테스트용 정적 팩토리 메서드
     * userId를 명시적으로 설정할 수 있습니다.
     */
    public static User createForTest(Long userId, String loginId, String name, String password,
                                    Country country, Gender gender, LocalDate birthday) {
        User user = User.builder()
                .loginId(loginId)
                .name(name)
                .password(password)
                .country(country)
                .gender(gender)
                .birthday(birthday)
                .provider(Provider.LOCAL)
                .build();
        user.userId = userId;
        return user;
    }

    private static Country parseCountry(String country) {
        return EnumConverter.stringToEnum(country, Country.class, ErrorCode.INVALID_INPUT_VALUE);
    }

    private static Gender parseGender(String gender) {
        return EnumConverter.stringToEnum(gender, Gender.class, ErrorCode.INVALID_INPUT_VALUE);
    }

    private static LocalDate parseBirthday(String birthday) {
        try {
            return LocalDate.parse(birthday);
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
