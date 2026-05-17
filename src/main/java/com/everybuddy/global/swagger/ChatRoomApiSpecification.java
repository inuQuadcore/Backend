package com.everybuddy.global.swagger;

import com.everybuddy.domain.chatroom.dto.ChatRoomResponse;
import com.everybuddy.domain.chatroom.dto.CreateChatRoomRequest;
import com.everybuddy.domain.chatroom.dto.InviteMembersRequest;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@Tag(name = "채팅방 API", description = "채팅방 생성 및 조회 기능")
public interface ChatRoomApiSpecification {

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성하고 참여자를 초대합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "채팅방 생성 성공",
                    content = @Content(
                            schema = @Schema(implementation = ChatRoomResponse.class),
                            examples = @ExampleObject("""
                    {
                        "chatRoomId": 1,
                        "roomName": "스터디 그룹",
                        "createdAt": "2026-01-12T10:30:00",
                        "participantIds": [1, 2, 3],
                        "unreadCount": 0
                    }
                    """
                            )
                    )
            ),
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
                            "roomName": "채팅방 이름을 입력해주세요.",
                            "participantIds": "채팅방 참여자를 선택해주세요."
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
                    responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "USER_NOT_FOUND",
                        "message": "해당 유저를 찾을 수 없습니다."
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<ChatRoomResponse> createChatRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateChatRoomRequest request
    );

    @Operation(summary = "내 채팅방 목록 조회", description = "내가 참여 중인 모든 채팅방 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ChatRoomResponse.class),
                            examples = @ExampleObject("""
                    [
                        {
                            "chatRoomId": 1,
                            "roomName": "스터디 그룹",
                            "createdAt": "2026-01-12T10:30:00",
                            "participantIds": [1, 2, 3],
                            "unreadCount": 5
                        },
                        {
                            "chatRoomId": 2,
                            "roomName": "프로젝트 팀",
                            "createdAt": "2026-01-11T15:20:00",
                            "participantIds": [1, 4, 5],
                            "unreadCount": 0
                        }
                    ]
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
            )
    })
    ResponseEntity<List<ChatRoomResponse>> getMyChatRooms(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "채팅방 나가기", description = "내가 해당 채팅방에서 나갑니다. 다른 참여자는 그대로 유지되고 채팅방 자체도 남습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "채팅방 나가기 성공"),
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
                    responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 403,
                        "name": "USER_NOT_IN_CHATROOM",
                        "message": "채팅방에 참여하고 있지 않습니다."
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<Void> leaveChatRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long chatRoomId
    );

    @Operation(summary = "채팅방 멤버 초대", description = "기존 채팅방에 한 명 이상의 새 멤버를 초대합니다. 채팅방 참여자라면 누구나 초대할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "초대 성공"),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력 / 자기 자신 초대",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 400,
                        "name": "CANNOT_INVITE_SELF",
                        "message": "자기 자신을 초대할 수 없습니다."
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
                    responseCode = "403", description = "초대자가 채팅방 참여자가 아님",
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
                    responseCode = "404", description = "채팅방/유저를 찾을 수 없음 (차단 관계 포함)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "USER_NOT_FOUND",
                        "message": "해당 유저를 찾을 수 없습니다."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409", description = "이미 채팅방에 있는 사용자",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 409,
                        "name": "ALREADY_IN_CHATROOM",
                        "message": "이미 채팅방에 참여 중인 사용자입니다."
                    }
                    """
                            )
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
                    """
                            )
                    )
            )
    })
    ResponseEntity<Void> inviteMembers(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody InviteMembersRequest request
    );
}
