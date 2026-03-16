package com.everybuddy.domain.friendrelation.dto;

import com.everybuddy.domain.user.dto.UserLanguageResponse;
import com.everybuddy.domain.user.dto.UserTagResponse;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "친구 프로필 정보")
public class FriendResponse {

    @Schema(description = "유저 ID", example = "1")
    private final Long userId;

    @Schema(description = "이름", example = "홍길동")
    private final String name;

    @Schema(description = "프로필 이미지 URL", example = "https://everybuddy.s3.amazonaws.com/profile/1.jpg")
    private final String profileImageUrl;

    @Schema(description = "국적", example = "KOREA")
    private final String country;

    @Schema(description = "자기소개", example = "안녕하세요!")
    private final String bio;

    @Schema(description = "언어 목록")
    private final List<UserLanguageResponse> languages;

    @Schema(description = "태그 목록")
    private final List<UserTagResponse> tags;

    @Builder
    private FriendResponse(Long userId, String name, String profileImageUrl, String country,
                           String bio, List<UserLanguageResponse> languages, List<UserTagResponse> tags) {
        this.userId = userId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.country = country;
        this.bio = bio;
        this.languages = languages;
        this.tags = tags;
    }

    public static FriendResponse of(User user, String profileImageUrl,
                                    List<UserLanguage> languages, List<UserTag> tags) {
        return FriendResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .profileImageUrl(profileImageUrl)
                .country(user.getCountry().name())
                .bio(user.getBio())
                .languages(languages.stream().map(UserLanguageResponse::from).toList())
                .tags(tags.stream().map(UserTagResponse::from).toList())
                .build();
    }
}
