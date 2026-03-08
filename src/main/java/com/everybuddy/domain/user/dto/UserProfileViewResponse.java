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

    @Schema(description = "성별", example = "MALE")
    private final String gender;

    @Schema(description = "자기소개", example = "안녕하세요!")
    private final String bio;

    @Builder
    private UserProfileViewResponse(String profileImageUrl, String country, String name, Integer age, String gender, String bio) {
        this.profileImageUrl = profileImageUrl;
        this.country = country;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.bio = bio;
    }

    public static UserProfileViewResponse from(User user, String profileImageUrl) {
        return UserProfileViewResponse.builder()
                .profileImageUrl(profileImageUrl)
                .country(user.getCountry().name())
                .name(user.getName())
                .age(Period.between(user.getBirthday(), LocalDate.now()).getYears())
                .gender(user.getGender().name())
                .bio(user.getBio())
                .build();
    }
}
