package com.everybuddy.domain.user.dto;

import com.everybuddy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "유저 프로필 응답")
public class UserProfileResponse {

    @Schema(description = "유저 ID", example = "1")
    private Long userId;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "생년월일", example = "2000-01-01")
    private LocalDate birthday;

    @Schema(description = "성별", example = "MALE")
    private String gender;

    @Schema(description = "국적", example = "KOREA")
    private String country;

    @Schema(description = "자기소개", example = "안녕하세요!")
    private String bio;

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
