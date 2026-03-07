package com.everybuddy.domain.user.dto;

import com.everybuddy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "유저 프로필 응답")
public class UserProfileResponse {

    @Schema(description = "유저 ID", example = "1")
    private final Long userId;

    @Schema(description = "이름", example = "홍길동")
    private final String name;

    @Schema(description = "프로필 이미지 URL")
    private final String profileImageUrl;

    @Schema(description = "생년월일", example = "2000-01-01")
    private final LocalDate birthday;

    @Schema(description = "성별", example = "MALE")
    private final String gender;

    @Schema(description = "국적", example = "KOREA")
    private final String country;

    @Schema(description = "자기소개", example = "안녕하세요!")
    private final String bio;

    @Builder
    private UserProfileResponse(Long userId, String name, String profileImageUrl, LocalDate birthday, String gender, String country, String bio) {
        this.userId = userId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.birthday = birthday;
        this.gender = gender;
        this.country = country;
        this.bio = bio;
    }

    public static UserProfileResponse from(User user, String profileImageUrl) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .profileImageUrl(profileImageUrl)
                .birthday(user.getBirthday())
                .gender(user.getGender().name())
                .country(user.getCountry().name())
                .bio(user.getBio())
                .build();
    }
}
