package com.everybuddy.domain.chatroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "채팅방 생성 요청")
public class CreateChatRoomRequest {

    @Schema(description = "채팅방 이름", example = "스터디 그룹")
    @NotBlank(message = "채팅방 이름을 입력해주세요.")
    private String roomName;

    @Schema(description = "그룹 채팅 여부 (false: 1:1, true: 그룹). 클라가 UI에 따라 결정.", example = "true")
    @NotNull(message = "채팅방 종류를 선택해주세요.")
    private Boolean isGroup;

    @Schema(description = "참여자 ID 목록 (본인 제외)", example = "[2, 3, 4]")
    @NotEmpty(message = "채팅방 참여자를 선택해주세요.")
    private List<Long> participantIds;

    private CreateChatRoomRequest() {}

    @Builder
    private CreateChatRoomRequest(String roomName, Boolean isGroup, List<Long> participantIds) {
        this.roomName = roomName;
        this.isGroup = isGroup;
        this.participantIds = participantIds;
    }

    public static CreateChatRoomRequest ofForTest(String roomName, boolean isGroup, List<Long> participantIds) {
        return CreateChatRoomRequest.builder()
                .roomName(roomName)
                .isGroup(isGroup)
                .participantIds(participantIds)
                .build();
    }
}
