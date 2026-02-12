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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "메시지 API", description = "채팅 메시지 전송·삭제·읽음 처리 기능")
public interface MessageApiSpecification {

    @Operation(summary = "메시지 전송", description = "텍스트 또는 파일 메시지를 전송합니다. 파일이 포함되면 파일 메시지, 없으면 텍스트 메시지로 처리됩니다.")
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
            @Parameter(
                    description = """
                            전송할 파일 (선택적, 파일 포함 시 파일 메시지로 처리)
                            - 최대 크기: 10MB
                            - 이미지: jpg, jpeg, png, gif, webp, heic
                            - 비디오: mp4, mov, avi, webm
                            - 오디오: mp3, wav, m4a, aac
                            - 문서: pdf, txt, doc, docx, xls, xlsx, ppt, pptx
                            - 압축: zip, rar
                            """,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(required = false) MultipartFile file
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
}
