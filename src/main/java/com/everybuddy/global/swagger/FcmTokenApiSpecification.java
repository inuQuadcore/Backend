package com.everybuddy.global.swagger;

import com.everybuddy.domain.fcmtoken.dto.FcmTokenRegisterRequest;
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
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "FCM 토큰 API", description = "FCM 푸시 알림용 디바이스 토큰 등록/삭제")
public interface FcmTokenApiSpecification {

    @Operation(summary = "FCM 토큰 등록", description = """
            현재 로그인한 사용자의 FCM 디바이스 토큰을 등록합니다. 한 사용자당 하나의 토큰만 보관되며, 이미 토큰이 있으면 새 값으로 덮어씁니다 (upsert).

            **FE 호출 시점**
            - 앱 최초 시작 후 FCM 토큰을 발급받자마자 1회 호출합니다.
            - FCM SDK의 토큰 갱신 콜백(`onTokenRefresh` / `getToken()` 결과 변경) 시마다 다시 호출합니다.
            - 알림 권한을 사용자가 허용한 직후에도 호출이 필요할 수 있습니다.
            - 동일 토큰을 여러 번 호출해도 안전합니다 (upsert).

            **호출 안 하면 생기는 문제**
            - 토큰이 백엔드에 없으면 푸시가 절대 가지 않습니다. 인앱 알림은 보이는데 푸시가 안 오는 증상이 발생합니다.

            **푸시 수신 시 data 페이로드 형식**
            FCM 푸시의 `data` 페이로드는 클라이언트가 알림 클릭 시 라우팅에 사용합니다. notification(title/body)은 OS 트레이가 자동 표시하고, data는 앱에서 코드로 처리합니다.

            - 친구추가 알림: `{ "fromUserId": "<친구 추가한 사용자 ID>" }`
              - 클릭 시 fromUserId로 친구 프로필 화면 라우팅 권장.
            - 채팅 메시지 알림: `{ "chatRoomId": "<채팅방 ID>", "messageId": "<메시지 ID>", "senderId": "<발신자 ID>" }`
              - 클릭 시 chatRoomId로 해당 채팅방 라우팅 권장.

            모든 값은 문자열로 전달됩니다 (FCM data 제약). FE에서 필요 시 숫자로 파싱하세요.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FCM 토큰 등록 성공"),
            @ApiResponse(
                    responseCode = "400", description = "토큰 누락",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 400,
                        "name": "MISSING_PARAMETER",
                        "message": "요청에 필요한 입력이 누락되었습니다."
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
                    responseCode = "404", description = "유저 없음",
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
    ResponseEntity<Void> register(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody FcmTokenRegisterRequest request
    );

    @Operation(summary = "FCM 토큰 삭제", description = """
            현재 로그인한 사용자의 FCM 토큰을 백엔드에서 제거합니다. 등록된 토큰이 없어도 204를 반환합니다 (멱등).

            **FE 호출 시점**
            - 사용자가 로그아웃할 때 호출합니다.
            - 계정 전환/탈퇴 시에도 호출합니다.

            **호출 안 하면 생기는 문제**
            - 같은 디바이스에서 다른 계정으로 로그인하면 이전 사용자에게 푸시가 갑니다.
            - 토큰이 만료되면 백엔드에서도 자동 정리되긴 하지만, 그 전까지 잘못된 발송이 발생할 수 있습니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "FCM 토큰 삭제 성공 (등록된 토큰이 없어도 동일하게 204)"),
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
                    responseCode = "404", description = "유저 없음",
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
    ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );
}
