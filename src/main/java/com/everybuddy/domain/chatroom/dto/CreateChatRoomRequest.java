package com.everybuddy.domain.chatroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "채팅방 생성 요청")
public class CreateChatRoomRequest {

    @Schema(description = "채팅방 이름", example = "스터디 그룹")
    @NotBlank(message = "채팅방 이름을 입력해주세요.")
    private String roomName;

    @Schema(description = "참여자 ID 목록", example = "[2, 3, 4]")
    @NotEmpty(message = "채팅방 참여자를 선택해주세요.")
    private List<Long> participantIds;
}
