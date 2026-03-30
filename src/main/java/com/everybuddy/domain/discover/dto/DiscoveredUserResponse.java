package com.everybuddy.domain.discover.dto;

import com.everybuddy.domain.user.dto.UserLanguageResponse;
import com.everybuddy.domain.user.dto.UserTagResponse;
import com.everybuddy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Schema(description = "탐색된 유저 프로필")
public class DiscoveredUserResponse {

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

    @Schema(description = "마지막 접속 시각")
    private final LocalDateTime lastSeenAt;

    @Builder
    private DiscoveredUserResponse(Long userId, String name, String profileImageUrl, String country,
                                   String bio, List<UserLanguageResponse> languages,
                                   List<UserTagResponse> tags, LocalDateTime lastSeenAt) {
        this.userId = userId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.country = country;
        this.bio = bio;
        this.languages = languages;
        this.tags = tags;
        this.lastSeenAt = lastSeenAt;
    }

    public static DiscoveredUserResponse of(User user, String profileImageUrl,
                                            List<UserLanguageResponse> languages,
                                            List<UserTagResponse> tags,
                                            LocalDateTime lastSeenAt) {
        return DiscoveredUserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .profileImageUrl(profileImageUrl)
                .country(user.getCountry().name())
                .bio(user.getBio())
                .languages(languages)
                .tags(tags)
                .lastSeenAt(lastSeenAt)
                .build();
    }
}
