package com.everybuddy.global.swagger;

import com.everybuddy.domain.statusmessage.dto.CreateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.dto.FriendStatusMessageListResponse;
import com.everybuddy.domain.statusmessage.dto.MyStatusMessageResponse;
import com.everybuddy.domain.statusmessage.dto.UpdateStatusMessageRequest;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "상태메시지 API", description = "상태메시지 관련 기능")
public interface StatusMessageApiSpecification {

    @Operation(summary = "상태메시지 작성", description = "상태메시지를 작성합니다. 인당 하나만 작성 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태메시지 작성 성공"),
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
                    responseCode = "409", description = "이미 상태메시지 존재",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 409,
                        "name": "STATUS_MESSAGE_ALREADY_EXISTS",
                        "message": "이미 상태메시지가 존재합니다."
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
    ResponseEntity<Void> createStatusMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CreateStatusMessageRequest request
    );

    @Operation(summary = "상태메시지 수정", description = "상태메시지를 수정합니다. 24시간 이내에만 수정 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태메시지 수정 성공"),
            @ApiResponse(
                    responseCode = "400", description = "24시간 만료된 상태메시지",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 400,
                        "name": "STATUS_MESSAGE_EXPIRED",
                        "message": "만료된 상태메시지입니다."
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
                    responseCode = "404", description = "상태메시지 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "STATUS_MESSAGE_NOT_FOUND",
                        "message": "상태메시지를 찾을 수 없습니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<Void> updateStatusMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UpdateStatusMessageRequest request
    );

    @Operation(summary = "상태메시지 삭제", description = "상태메시지를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "상태메시지 삭제 성공"),
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
                    responseCode = "404", description = "상태메시지 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "STATUS_MESSAGE_NOT_FOUND",
                        "message": "상태메시지를 찾을 수 없습니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<Void> deleteStatusMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "내 상태메시지 조회", description = "본인의 상태메시지를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = MyStatusMessageResponse.class),
                            examples = @ExampleObject("""
                    {
                        "statusMessageId": 42,
                        "content": "오늘도 화이팅!",
                        "updatedAt": "2026-05-13T11:30:00"
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
                    responseCode = "404", description = "상태메시지 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "STATUS_MESSAGE_NOT_FOUND",
                        "message": "상태메시지를 찾을 수 없습니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<MyStatusMessageResponse> getMyStatusMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "친구 상태메시지 목록 조회", description = """
            친구들의 상태메시지를 최신순으로 조회합니다. 무한스크롤 방식으로 동작합니다.

            **페이지네이션 사용 방법**
            - 첫 요청은 cursor 없이 호출합니다.
            - 응답의 nextCursor를 다음 요청의 cursor로 사용합니다.
            - hasNext=false이면 마지막 페이지입니다. 추가 요청은 불필요합니다.

            **본인 상태메시지 표시**
            - 본인 상태메시지는 이 API에 포함되지 않습니다.
            - 목록 최상단에 본인 상태메시지를 표시하려면 GET /api/v1/status-messages/me를 별도 호출하여 첫 번째 항목으로 추가해야 합니다.
            """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = FriendStatusMessageListResponse.class),
                            examples = @ExampleObject("""
                    {
                        "statusMessages": [
                            {
                                "statusMessageId": 42,
                                "profileImageUrl": "https://cdn.example.com/profiles/user-1/photo.jpg",
                                "nickname": "홍길동",
                                "content": "오늘도 화이팅!",
                                "updatedAt": "2026-05-13T11:30:00"
                            },
                            {
                                "statusMessageId": 38,
                                "profileImageUrl": null,
                                "nickname": "김철수",
                                "content": "날씨가 너무 좋다",
                                "updatedAt": "2026-05-12T14:20:00"
                            }
                        ],
                        "nextCursor": 38,
                        "hasNext": false
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
                    responseCode = "404", description = "커서에 해당하는 상태메시지 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "STATUS_MESSAGE_NOT_FOUND",
                        "message": "상태메시지를 찾을 수 없습니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<FriendStatusMessageListResponse> getFriendStatusMessages(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "이전 페이지 마지막 statusMessageId (첫 페이지는 생략)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기 (기본값 20)") @RequestParam(defaultValue = "20") int size
    );
}
