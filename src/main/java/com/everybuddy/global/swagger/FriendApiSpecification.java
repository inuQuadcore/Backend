package com.everybuddy.global.swagger;

import com.everybuddy.domain.friendrelation.dto.FriendListResponse;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "친구 API", description = "친구 관계 관련 기능")
public interface FriendApiSpecification {

    @Operation(summary = "친구 목록 조회", description = "내 친구 전체 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = FriendListResponse.class),
                            examples = @ExampleObject("""
                    {
                        "friends": [
                            {
                                "userId": 1,
                                "name": "홍길동",
                                "profileImageUrl": "https://everybuddy.s3.amazonaws.com/profile/1.jpg",
                                "country": "KOREA",
                                "bio": "안녕하세요!",
                                "languages": [
                                    { "language": "ENGLISH", "level": 3 }
                                ],
                                "tags": [
                                    { "tag": "SPORTS", "category": "HOBBY" }
                                ]
                            }
                        ]
                    }
                    """)
                    )),
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
            )
    })
    ResponseEntity<FriendListResponse> getFriends(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "친구 추가", description = "특정 유저를 친구로 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 추가 성공"),
            @ApiResponse(
                    responseCode = "400", description = "자기 자신을 친구로 추가",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 400,
                        "name": "CANNOT_ADD_SELF",
                        "message": "자기 자신을 친구로 추가할 수 없습니다."
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
                    responseCode = "409", description = "이미 친구인 사용자",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 409,
                        "name": "ALREADY_FRIEND",
                        "message": "이미 친구인 사용자입니다."
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
    ResponseEntity<Void> addFriend(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long toUserId
    );
}
