package com.everybuddy.global.swagger;

import com.everybuddy.domain.user.dto.UpdateProfileRequest;
import com.everybuddy.domain.user.dto.UpdateTagsRequest;
import com.everybuddy.domain.user.dto.UserLanguageRequest;
import com.everybuddy.domain.user.dto.UserProfileResponse;
import com.everybuddy.domain.user.dto.UserTagResponse;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "유저 API", description = "유저 프로필 관련 기능")
public interface UserApiSpecification {

    @Operation(summary = "유저 태그 조회", description = "특정 유저의 태그 목록을 조회합니다. 본인 userId를 전달하면 본인 태그를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserTagResponse.class)))
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
                    responseCode = "410", description = "탈퇴한 유저",
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
            )
    })
    ResponseEntity<List<UserTagResponse>> getUserTags(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long userId
    );

    @Operation(summary = "태그 수정", description = "유저의 태그 목록을 전달된 목록으로 교체합니다. 빈 리스트 전송 시 전체 삭제됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 400,
                        "name": "INVALID_INPUT_VALUE",
                        "message": "잘못된 입력입니다."
                    }
                    """)
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
                    responseCode = "410", description = "탈퇴한 유저",
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
            )
    })
    ResponseEntity<Void> updateTags(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateTagsRequest request
    );

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 유저를 탈퇴 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 성공"),
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
                    responseCode = "410", description = "이미 탈퇴한 유저",
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
            )
    })
    ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "관심 언어 수준 수정", description = "관심 언어 목록에 있는 언어의 수준(1~5)을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "잘못된 언어 값", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다."
                                    }
                                    """),
                                    @ExampleObject(name = "수준 범위 초과", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다.",
                                        "errors": {
                                            "level": "언어 수준은 1 이상이어야 합니다."
                                        }
                                    }
                                    """),
                                    @ExampleObject(name = "수준 범위 초과", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다.",
                                        "errors": {
                                            "level": "언어 수준은 5 이하여야 합니다."
                                        }
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
                    responseCode = "404", description = "관심 언어 목록에 없는 언어",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "USER_LANGUAGE_NOT_FOUND",
                        "message": "해당 언어가 관심 언어 목록에 없습니다."
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410", description = "탈퇴한 유저",
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
            )
    })
    ResponseEntity<Void> updateLanguageLevel(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UserLanguageRequest request
    );

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
