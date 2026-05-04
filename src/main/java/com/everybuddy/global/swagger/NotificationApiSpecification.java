package com.everybuddy.global.swagger;

import com.everybuddy.domain.notification.dto.HasUnreadResponse;
import com.everybuddy.domain.notification.dto.NotificationListResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "알림 API", description = "인앱 알림 조회/읽음 처리")
public interface NotificationApiSpecification {

    @Operation(summary = "알림 리스트 조회", description = """
            현재 사용자의 알림을 최신순으로 조회합니다. 무한스크롤(커서 페이지네이션) 방식.

            **페이지네이션 사용 방법**
            - 첫 요청은 before 없이 호출합니다.
            - 응답의 nextCursor를 다음 요청의 before로 사용합니다.
            - hasNext=false이면 마지막 페이지입니다.

            **노출되는 알림 종류**
            - 친구추가 알림 (`FRIEND_ADDED`): 누군가 본인을 친구로 추가했을 때.
            - 채팅 메시지는 이 리스트에 포함되지 않습니다 (FCM 푸시만 발송, DB 저장 X).
            """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = NotificationListResponse.class),
                            examples = @ExampleObject("""
                    {
                        "notifications": [
                            {
                                "notificationId": 42,
                                "type": "FRIEND_ADDED",
                                "title": "새로운 친구",
                                "body": "홍길동님이 친구로 추가했어요.",
                                "timeAgo": "5분 전",
                                "isRead": false
                            },
                            {
                                "notificationId": 38,
                                "type": "FRIEND_ADDED",
                                "title": "새로운 친구",
                                "body": "김철수님이 친구로 추가했어요.",
                                "timeAgo": "2일 전",
                                "isRead": true
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
            )
    })
    ResponseEntity<NotificationListResponse> getList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "이전 페이지 마지막 notificationId (첫 페이지는 생략)") @RequestParam(required = false) Long before,
            @Parameter(description = "페이지 크기 (기본값 10)") @RequestParam(defaultValue = "10") int limit
    );

    @Operation(summary = "미읽 알림 존재 여부", description = """
            현재 사용자에게 읽지 않은 알림이 하나라도 있는지 boolean으로 반환합니다. 홈 화면 등에서 빨간 점(미읽 표시) 노출 여부 판단용 경량 API.

            **FE 호출 시점**
            - 홈/메인 진입 시.
            - 알림 리스트를 닫고 다른 화면으로 돌아갈 때.
            - 푸시 수신 시 자동 갱신 가능.

            개수가 필요하면 별도 API 없음 — 알림 리스트 조회로 처리.
            """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = HasUnreadResponse.class),
                            examples = @ExampleObject("""
                    {
                        "hasUnread": true
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
            )
    })
    ResponseEntity<HasUnreadResponse> hasUnread(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "알림 단건 읽음 처리", description = """
            특정 알림을 읽음 처리합니다. 이미 읽은 알림은 그대로 둡니다 (멱등). 본인 알림이 아니면 404 반환.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
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
                    responseCode = "404", description = "알림 없음 또는 본인 알림 아님",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "NOTIFICATION_NOT_FOUND",
                        "message": "알림을 찾을 수 없습니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<Void> markRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long notificationId
    );

    @Operation(summary = "알림 전체 읽음 처리", description = """
            현재 사용자의 모든 미읽 알림을 한 번에 읽음 처리합니다. 미읽 알림이 없어도 204를 반환합니다 (멱등).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "전체 읽음 처리 성공"),
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
            )
    })
    ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );
}
