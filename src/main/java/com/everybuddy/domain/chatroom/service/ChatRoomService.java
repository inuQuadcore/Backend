package com.everybuddy.domain.chatroom.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.dto.ChatRoomResponse;
import com.everybuddy.domain.chatroom.dto.CreateChatRoomRequest;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatPartRepository chatPartRepository;
    private final UserRepository userRepository;
    private final FirebaseDatabase firebaseDatabase;

    @Transactional
    public ChatRoomResponse createChatRoom(Long creatorId, CreateChatRoomRequest request) {
        ChatRoom chatRoom = ChatRoom.create(request.getRoomName());
        chatRoomRepository.save(chatRoom);

        User user = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 성능 개선 필요
        List<Long> allParticipantIds = addAllParticipants(user, chatRoom, request.getParticipantIds());

        saveParticipantsToFirebase(chatRoom.getChatRoomId(), allParticipantIds);

        return ChatRoomResponse.from(chatRoom, allParticipantIds);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(Long userId) {
        List<ChatPart> myChatParts = chatPartRepository.findByUserIdWithChatRoom(userId);

        if (myChatParts.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Long>> participantsMap = findParticipantsMapByChatRooms(myChatParts);

        return buildChatRoomResponses(myChatParts, participantsMap);
    }

    private List<Long> addAllParticipants(User creator, ChatRoom chatRoom, List<Long> otherParticipantIds) {
        ChatPart creatorPart = ChatPart.create(creator, chatRoom);
        chatPartRepository.save(creatorPart);

        List<ChatPart> otherChatParts = saveOtherParticipants(chatRoom, otherParticipantIds);

        return extractAllParticipantIds(creatorPart, otherChatParts);
    }


    private List<ChatPart> saveOtherParticipants(ChatRoom chatRoom, List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return List.of();
        }

        List<User> participants = userRepository.findAllById(participantIds);
        List<ChatPart> chatParts = participants.stream()
                .map(user -> ChatPart.create(user, chatRoom))
                .toList();

        return chatPartRepository.saveAll(chatParts);
    }

    private List<Long> extractAllParticipantIds(ChatPart creatorPart, List<ChatPart> otherChatParts) {
        List<Long> allParticipantIds = new ArrayList<>();
        allParticipantIds.add(creatorPart.getUser().getUserId());
        allParticipantIds.addAll(otherChatParts.stream()
                .map(chatPart -> chatPart.getUser().getUserId())
                .toList());
        return allParticipantIds;
    }

    private Map<Long, List<Long>> findParticipantsMapByChatRooms(List<ChatPart> myChatParts) {
        List<Long> chatRoomIds = extractChatRoomIds(myChatParts);
        List<ChatPart> allParticipants = chatPartRepository.findByChatRoomIdsWithUser(chatRoomIds);

        return groupParticipantsByChatRoom(allParticipants);
    }

    private List<Long> extractChatRoomIds(List<ChatPart> chatParts) {
        return chatParts.stream()
                .map(chatPart -> chatPart.getChatRoom().getChatRoomId())
                .toList();
    }

    private Map<Long, List<Long>> groupParticipantsByChatRoom(List<ChatPart> allParticipants) {
        return allParticipants.stream()
                .collect(Collectors.groupingBy(
                        chatPart -> chatPart.getChatRoom().getChatRoomId(),
                        Collectors.mapping(
                                chatPart -> chatPart.getUser().getUserId(),
                                Collectors.toList()
                        )
                ));
    }

    private List<ChatRoomResponse> buildChatRoomResponses(
            List<ChatPart> myChatParts,
            Map<Long, List<Long>> participantsMap) {

        return myChatParts.stream()
                .map(chatPart -> ChatRoomResponse.from(
                        chatPart.getChatRoom(),
                        participantsMap.get(chatPart.getChatRoom().getChatRoomId())
                ))
                .toList();
    }

    private void saveParticipantsToFirebase(Long chatRoomId, List<Long> participantIds) {
        DatabaseReference participantsRef = firebaseDatabase.getReference("chatrooms")
                .child(String.valueOf(chatRoomId))
                .child("participants");

        Map<String, Boolean> participantsMap = createParticipantsMap(participantIds);
        participantsRef.setValueAsync(participantsMap);
    }

    private Map<String, Boolean> createParticipantsMap(List<Long> participantIds) {
        Map<String, Boolean> participantsMap = new HashMap<>();
        for (Long participantId : participantIds) {
            participantsMap.put(String.valueOf(participantId), true);
        }
        return participantsMap;
    }
}
