package com.everybuddy.domain.chatroom.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CreateChatRoomRequest {

    private String roomName;

    private List<Long> participantIds;
}
