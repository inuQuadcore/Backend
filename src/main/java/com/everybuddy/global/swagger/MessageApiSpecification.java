package com.everybuddy.global.swagger;

import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.message.dto.MessageResponse;
import com.everybuddy.domain.message.dto.MessageSyncResponse;
import com.everybuddy.domain.message.dto.UpdateMessageRequest;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Tag(name = "메시지 API", description = "채팅 메시지 전송·삭제·읽음 처리 기능")
public interface MessageApiSpecification {

    @Operation(
            summary = "메시지 전송",
            description = "텍스트 또는 파일 메시지를 전송합니다. 파일이 포함되면 파일 메시지, 없으면 텍스트 메시지로 처리됩니다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = SendMessageMultipart.class),
                            encoding = {
                                    @Encoding(
                                            name = "request",
                                            contentType = MediaType.APPLICATION_JSON_VALUE
                                    ),
                                    @Encoding(
                                            name = "file",
                                            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "메시지 전송 성공"),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "필수 필드 누락", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다.",
                                        "errors": {
                                            "chatRoomId": "메시지를 전송할 채팅방을 선택해주세요.",
                                            "content": "메시지 본문을 입력해주세요."
                                        }
                                    }
                                    """),
                                    @ExampleObject(name = "지원하지 않는 파일 형식", value = """
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
            ),
            @ApiResponse(
                    responseCode = "413", description = "파일 크기 초과",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 413,
                        "name": "FILE_SIZE_EXCEEDED",
                        "message": "파일 크기가 제한을 초과했습니다. (최대 10MB)"
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<Void> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestPart("request") ChatMessageRequest request,
            @RequestPart(required = false) MultipartFile file
    );

    @Operation(summary = "메시지 수정", description = "자신이 전송한 텍스트 메시지를 수정합니다. 전송 후 5분 이내에만 수정 가능합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "메시지 수정 성공",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력 또는 파일 메시지 수정 시도",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "본문 누락", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다.",
                                        "errors": {
                                            "content": "메시지 본문을 입력해주세요."
                                        }
                                    }
                                    """),
                                    @ExampleObject(name = "파일 메시지 수정 불가", value = """
                                    {
                                        "code": 400,
                                        "name": "CANNOT_EDIT_FILE_MESSAGE",
                                        "message": "파일 메시지는 수정할 수 없습니다."
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
                    responseCode = "403", description = "수정 권한 없음 또는 시간 초과",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "타인 메시지", value = """
                                    {
                                        "code": 403,
                                        "name": "NOT_MESSAGE_OF_USER",
                                        "message": "자신의 메시지만 수정/삭제할 수 있습니다."
                                    }
                                    """),
                                    @ExampleObject(name = "5분 초과", value = """
                                    {
                                        "code": 403,
                                        "name": "MESSAGE_EDIT_TIME_EXCEEDED",
                                        "message": "메시지 수정/삭제 가능 시간이 지났습니다. (최대 5분)"
                                    }
                                    """)
                            }
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
                    """)
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
                    """)
                    )
            )
    })
    ResponseEntity<MessageResponse> updateMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "수정할 메시지 ID", required = true) @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request
    );

    @Operation(summary = "메시지 삭제", description = "자신이 전송한 메시지를 삭제합니다. 전송 후 5분 이내에만 삭제 가능합니다.")
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
                    responseCode = "403", description = "메시지 삭제 권한 없음 또는 시간 초과",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "타인 메시지", value = """
                                    {
                                        "code": 403,
                                        "name": "NOT_MESSAGE_OF_USER",
                                        "message": "자신의 메시지만 수정/삭제할 수 있습니다."
                                    }
                                    """),
                                    @ExampleObject(name = "5분 초과", value = """
                                    {
                                        "code": 403,
                                        "name": "MESSAGE_EDIT_TIME_EXCEEDED",
                                        "message": "메시지 수정/삭제 가능 시간이 지났습니다. (최대 5분)"
                                    }
                                    """)
                            }
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

    @Operation(summary = "메시지 읽음 처리", description = "특정 메시지까지 읽음 처리합니다.")
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
            @Parameter(description = "읽음 처리할 마지막 메시지 ID", required = true) @PathVariable Long messageId
    );

    @Operation(
            summary = "채팅방 메시지 동기화",
            description = """
                    since 이후 새 메시지, 수정된 메시지, 삭제된 메시지 ID를 반환합니다. since가 없으면 전체 메시지를 반환합니다.

                    - since 형식: ISO 8601 (예: 2026-04-07T10:00:00)
                    - sendAt 형식: ISO 8601 LocalDateTime (REST API 기준)
                    - 참고: Firebase Realtime DB의 sendAt은 epoch milliseconds로 별도 관리됩니다. (수정 예정)
                    - 채팅방에 접속할 때 한 번 요청해서 로컬 DB와 서버 DB를 동기화하는 용도의 API입니다.
                    - 해당 API를 호출한 후 실시간으로 오는 메시지는 realtimeDB를 이용해 로컬 DB를 갱신해야 합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "동기화 성공",
                    content = @Content(
                            schema = @Schema(implementation = MessageSyncResponse.class),
                            examples = @ExampleObject("""
                    {
                        "newMessages": [
                            {
                                "messageId": 10,
                                "userId": 1,
                                "userName": "홍길동",
                                "messageType": "TEXT",
                                "content": "안녕하세요",
                                "sendAt": "2026-04-07T10:00:00",
                                "editedAt": null
                            }
                        ],
                        "updatedMessages": [
                            {
                                "messageId": 5,
                                "userId": 2,
                                "userName": "김철수",
                                "messageType": "TEXT",
                                "content": "수정된 내용",
                                "sendAt": "2026-04-07T09:55:00",
                                "editedAt": "2026-04-07T09:58:00"
                            }
                        ],
                        "deletedIds": [3, 4]
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
                    responseCode = "404", description = "채팅방을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "CHATROOM_NOT_FOUND",
                        "message": "해당 채팅방을 찾을 수 없습니다."
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<MessageSyncResponse> getMessages(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "채팅방 ID", required = true) @PathVariable Long chatRoomId,
            @Parameter(description = "마지막 동기화 시각 (없으면 전체 조회)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since
    );
}
