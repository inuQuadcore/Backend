package com.everybuddy.domain.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateChatRoomRequest {

    @NotBlank(message = "채팅방 이름을 입력해주세요.")
    private String roomName;

    @NotEmpty(message = "채팅방 참여자를 선택해주세요.")
    private List<Long> participantIds;
}
