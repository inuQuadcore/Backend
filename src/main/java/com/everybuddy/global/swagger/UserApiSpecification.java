package com.everybuddy.global.swagger;

import com.everybuddy.domain.user.dto.UpdateProfileRequest;
import com.everybuddy.domain.user.dto.UserProfileResponse;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "유저 API", description = "유저 프로필 관련 기능")
public interface UserApiSpecification {

    @Operation(
            summary = "프로필 수정",
            description = "이름, 생일, 성별, 국적, 자기소개, 프로필 이미지를 수정합니다. 모든 필드는 선택적이며, 전달된 필드만 수정됩니다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = UpdateProfileMultipart.class),
                            encoding = {
                                    @Encoding(
                                            name = "request",
                                            contentType = MediaType.APPLICATION_JSON_VALUE
                                    ),
                                    @Encoding(
                                            name = "profileImage",
                                            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "프로필 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "자기소개 150자 초과", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다.",
                                        "errors": {
                                            "bio": "자기소개는 150자 이내로 입력해주세요."
                                        }
                                    }
                                    """),
                                    @ExampleObject(name = "잘못된 성별/국적 값", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다."
                                    }
                                    """),
                                    @ExampleObject(name = "잘못된 날짜 형식", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다."
                                    }
                                    """),
                                    @ExampleObject(name = "지원하지 않는 이미지 형식", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_FILE_TYPE",
                                        "message": "지원하지 않는 파일 형식입니다."
                                    }
                                    """),
                                    @ExampleObject(name = "빈 파일", value = """
                                    {
                                        "code": 400,
                                        "name": "EMPTY_FILE",
                                        "message": "빈 파일은 업로드할 수 없습니다."
                                    }
                                    """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 401,
                        "name": "JWT_ENTRY_POINT",
                        "message": "로그인이 필요합니다."
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404", description = "유저를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "USER_NOT_FOUND",
                        "message": "해당 유저를 찾을 수 없습니다."
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410", description = "삭제된 사용자",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 410,
                        "name": "USER_DELETED",
                        "message": "삭제된 사용자입니다."
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "413", description = "이미지 크기 초과",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 413,
                        "name": "FILE_SIZE_EXCEEDED",
                        "message": "파일 크기가 제한을 초과했습니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestPart("request") UpdateProfileRequest request,
            @RequestPart(required = false) MultipartFile profileImage
    );
}
