package com.everybuddy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "프로필 수정 요청")
public class UpdateProfileRequest {

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "생년월일 (yyyy-MM-dd)", example = "2000-01-01")
    private String birthday;

    @Schema(description = "성별 (MALE, FEMALE, OTHER)", example = "MALE")
    private String gender;

    @Schema(description = "국적 (KOREA, USA, ...)", example = "KOREA")
    private String country;

    @Schema(description = "자기소개 (최대 150자)", example = "안녕하세요!")
    @Size(max = 150, message = "자기소개는 150자 이내로 입력해주세요.")
    private String bio;

    public static UpdateProfileRequest of(String name, String birthday, String gender, String country, String bio) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.name = name;
        request.birthday = birthday;
        request.gender = gender;
        request.country = country;
        request.bio = bio;
        return request;
    }
}
