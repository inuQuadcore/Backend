package com.everybuddy.domain.user.dto;

import com.everybuddy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Period;

@Getter
@Schema(description = "유저 프로필 조회 응답")
public class UserProfileViewResponse {

    @Schema(description = "프로필 이미지 URL")
    private final String profileImageUrl;

    @Schema(description = "국적", example = "KOREA")
    private final String country;

    @Schema(description = "이름", example = "홍길동")
    private final String name;

    @Schema(description = "나이", example = "25")
    private final Integer age;

    @Schema(description = "생년월일 (본인 조회 시에만 채워짐, 타인 조회 시 null)", example = "2000-01-01")
    private final LocalDate birthday;

    @Schema(description = "성별", example = "MALE")
    private final String gender;

    @Schema(description = "자기소개", example = "안녕하세요!")
    private final String bio;

    @Schema(description = "연속 출석일수", example = "5")
    private final int consecutiveDays;

    @Builder
    private UserProfileViewResponse(String profileImageUrl, String country, String name, Integer age, LocalDate birthday, String gender, String bio, int consecutiveDays) {
        this.profileImageUrl = profileImageUrl;
        this.country = country;
        this.name = name;
        this.age = age;
        this.birthday = birthday;
        this.gender = gender;
        this.bio = bio;
        this.consecutiveDays = consecutiveDays;
    }

    public static UserProfileViewResponse from(User user, String profileImageUrl, boolean isOwner, int consecutiveDays) {
        return UserProfileViewResponse.builder()
                .profileImageUrl(profileImageUrl)
                .country(user.getCountry().name())
                .name(user.getName())
                .age(Period.between(user.getBirthday(), LocalDate.now()).getYears())
                .birthday(isOwner ? user.getBirthday() : null)
                .gender(user.getGender().name())
                .bio(user.getBio())
                .consecutiveDays(consecutiveDays)
                .build();
    }
}
