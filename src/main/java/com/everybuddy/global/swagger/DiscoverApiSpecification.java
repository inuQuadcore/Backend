package com.everybuddy.global.swagger;

import com.everybuddy.domain.discover.dto.FilterDiscoverRequest;
import com.everybuddy.domain.discover.dto.FilterDiscoverResponse;
import com.everybuddy.domain.discover.dto.RandomDiscoverResponse;
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
import org.springframework.web.bind.annotation.ModelAttribute;

@Tag(name = "Discover API", description = "유저 탐색 관련 기능")
public interface DiscoverApiSpecification {

    @Operation(summary = "랜덤 유저 탐색", description = "친구·차단 관계를 제외한 랜덤 유저 6명을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "랜덤 유저 탐색 성공",
                    content = @Content(
                            schema = @Schema(implementation = RandomDiscoverResponse.class),
                            examples = @ExampleObject("""
                    {
                        "users": [
                            {
                                "userId": 2,
                                "name": "김철수",
                                "profileImageUrl": "https://everybuddy.s3.amazonaws.com/profile/2.jpg",
                                "country": "KOREA",
                                "bio": "반갑습니다!",
                                "languages": [{ "language": "ENGLISH", "level": 3 }],
                                "tags": [{ "tag": "WORKOUT", "category": "HOBBY" }],
                                "lastSeenAt": "2024-03-01T12:00:00"
                            }
                        ]
                    }
                    """)
                    )),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    { "code": 401, "name": "JWT_ENTRY_POINT", "message": "로그인이 필요합니다." }
                    """)
                    ))
    })
    ResponseEntity<RandomDiscoverResponse> getRandomUsers(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "필터 유저 탐색", description = """
            조건에 맞는 유저를 커서 기반으로 탐색합니다. 친구·차단 관계는 자동 제외됩니다.
            - isOnline: 현재 온라인 유저만
            - recentlyActive: 24시간 이내 접속 유저만
            - 두 조건 동시 적용 가능
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "필터 유저 탐색 성공",
                    content = @Content(
                            schema = @Schema(implementation = FilterDiscoverResponse.class),
                            examples = @ExampleObject("""
                    {
                        "users": [
                            {
                                "userId": 5,
                                "name": "이영희",
                                "profileImageUrl": null,
                                "country": "JAPAN",
                                "bio": "일본어 배우고 싶어요",
                                "languages": [{ "language": "JAPANESE", "level": 5 }],
                                "tags": [{ "tag": "TRAVEL", "category": "HOBBY" }],
                                "lastSeenAt": "2024-03-01T10:00:00"
                            }
                        ],
                        "hasNext": true,
                        "nextCursor": 5
                    }
                    """)
                    )),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    { "code": 401, "name": "JWT_ENTRY_POINT", "message": "로그인이 필요합니다." }
                    """)
                    )),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터 타입",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    { "code": 400, "name": "TYPE_MISMATCH", "message": "입력 형식이 올바르지 않습니다." }
                    """)
                    ))
    })
    ResponseEntity<FilterDiscoverResponse> getFilteredUsers(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute FilterDiscoverRequest request
    );
}
