package com.everybuddy.domain.chatroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "채팅방 멤버 초대 요청")
public class InviteMembersRequest {

    @Schema(description = "초대할 유저 ID 목록", example = "[5, 6]")
    @NotEmpty(message = "초대할 유저를 선택해주세요.")
    private List<Long> participantIds;

    private InviteMembersRequest() {}

    @Builder
    private InviteMembersRequest(List<Long> participantIds) {
        this.participantIds = participantIds;
    }

    public static InviteMembersRequest ofForTest(List<Long> participantIds) {
        return InviteMembersRequest.builder()
                .participantIds(participantIds)
                .build();
    }
}
