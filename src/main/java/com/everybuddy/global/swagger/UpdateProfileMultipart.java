package com.everybuddy.global.swagger;

import com.everybuddy.domain.user.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Schema(name = "UpdateProfileMultipart", description = "프로필 수정 멀티파트 요청")
public class UpdateProfileMultipart {

    @Schema(description = "프로필 수정 요청 (JSON)", implementation = UpdateProfileRequest.class)
    private UpdateProfileRequest request;

    @Schema(description = """
            프로필 이미지 (선택적)
            - 최대 크기: 5MB
            - 허용 형식: jpg, jpeg, png, gif, webp, heic
            """, type = "string", format = "binary")
    private MultipartFile profileImage;
}
