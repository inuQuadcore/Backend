package com.everybuddy.global.swagger;

import com.everybuddy.domain.message.dto.ChatMessageRequest;
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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "메시지 API", description = "채팅 메시지 전송·삭제·읽음 처리 기능")
public interface MessageApiSpecification {

    @Operation(summary = "메시지 전송", description = "채팅방에 새로운 메시지를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "메시지 전송 성공"),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 400,
                        "name": "INVALID_INPUT_VALUE",
                        "message": "잘못된 입력입니다.",
                        "errors": {
                            "chatRoomId": "메시지를 전송할 채팅방을 선택해주세요.",
                            "messageType": "메시지 타입을 확인해주세요.",
                            "content": "메시지 본문을 입력해주세요."
                        }
                    }
                    """
                            )
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
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403", description = "채팅방 접근 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 403,
                        "name": "USER_NOT_IN_CHATROOM",
                        "message": "해당 채팅방에 접근할 권한이 없습니다."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404", description = "사용자 또는 채팅방을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "사용자를 찾을 수 없음", value = """
                                    {
                                        "code": 404,
                                        "name": "USER_NOT_FOUND",
                                        "message": "해당 유저를 찾을 수 없습니다."
                                    }
                                    """),
                                    @ExampleObject(name = "채팅방을 찾을 수 없음", value = """
                                    {
                                        "code": 404,
                                        "name": "CHATROOM_NOT_FOUND",
                                        "message": "해당 채팅방을 찾을 수 없습니다."
                                    }
                                    """)
                            }
                    )
            )
    })
    ResponseEntity<Void> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChatMessageRequest request
    );

    @Operation(summary = "메시지 삭제", description = "자신이 전송한 메시지를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "메시지 삭제 성공"),
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
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403", description = "메시지 삭제 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 403,
                        "name": "NOT_MESSAGE_OF_USER",
                        "message": "자신의 메시지만 삭제할 수 있습니다."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404", description = "메시지를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "MESSAGE_NOT_FOUND",
                        "message": "해당 메시지를 찾을 수 없습니다."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409", description = "이미 삭제된 메시지",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 409,
                        "name": "MESSAGE_ALREADY_DELETED",
                        "message": "이미 삭제된 메시지입니다."
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "삭제할 메시지 ID", required = true) @PathVariable Long messageId
    );

    @Operation(summary = "메시지 읽음 처리", description = "특정 채팅방의 특정 메시지까지 읽음 처리합니다.")
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
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403", description = "채팅방 접근 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 403,
                        "name": "USER_NOT_IN_CHATROOM",
                        "message": "해당 채팅방에 접근할 권한이 없습니다."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404", description = "메시지를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "MESSAGE_NOT_FOUND",
                        "message": "해당 메시지를 찾을 수 없습니다."
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "채팅방 ID", required = true) @RequestParam Long chatRoomId,
            @Parameter(description = "읽음 처리할 마지막 메시지 ID", required = true) @PathVariable Long messageId
    );
}
