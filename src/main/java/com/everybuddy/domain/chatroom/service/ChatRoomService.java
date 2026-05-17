package com.everybuddy.domain.chatroom.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.dto.ChatRoomResponse;
import com.everybuddy.domain.chatroom.dto.CreateChatRoomRequest;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.event.ChatRoomCreatedEvent;
import com.everybuddy.domain.chatroom.event.ChatRoomLeftEvent;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatPartRepository chatPartRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatRoomResponse createChatRoom(Long creatorId, CreateChatRoomRequest request) {
        // 1. 검증
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (creator.isDeleted()) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }

        List<User> participants = validateParticipants(request.getParticipantIds());

        // 2. 저장
        ChatRoom chatRoom = ChatRoom.create(request.getRoomName());
        chatRoom = chatRoomRepository.save(chatRoom);

        List<Long> allParticipantIds = saveAllParticipants(creator, chatRoom, participants);

        // 3. Firebase: 커밋 성공 후 동기화
        eventPublisher.publishEvent(ChatRoomCreatedEvent.of(chatRoom.getChatRoomId(), allParticipantIds));

        return ChatRoomResponse.from(chatRoom, allParticipantIds);
    }

    @Transactional
    public void leaveChatRoom(Long userId, Long chatRoomId) {
        ChatPart chatPart = chatPartRepository.findByUserIdAndChatRoomId(userId, chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_IN_CHATROOM));

        chatPart.leave();

        eventPublisher.publishEvent(ChatRoomLeftEvent.of(chatRoomId, userId));
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

    private List<User> validateParticipants(List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return List.of();
        }

        List<User> participants = userRepository.findAllById(participantIds);

        if (participants.size() != participantIds.size()) {
            throw new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }

        boolean hasDeletedUser = participants.stream()
                .anyMatch(User::isDeleted);
        if (hasDeletedUser) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }

        return participants;
    }

    private List<Long> saveAllParticipants(User creator, ChatRoom chatRoom, List<User> participants) {
        ChatPart creatorPart = ChatPart.create(creator, chatRoom);
        chatPartRepository.save(creatorPart);

        List<ChatPart> otherChatParts = List.of();
        if (!participants.isEmpty()) {
            otherChatParts = participants.stream()
                    .map(user -> ChatPart.create(user, chatRoom))
                    .toList();
            chatPartRepository.saveAll(otherChatParts);
        }

        return extractAllParticipantIds(creatorPart, otherChatParts);
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

        List<ChatRoomResponse> responses = new ArrayList<>();

        for (ChatPart chatPart : myChatParts) {
            Long chatRoomId = chatPart.getChatRoom().getChatRoomId();
            Long lastReadMessageId = chatPart.getLastReadMessage() != null
                    ? chatPart.getLastReadMessage().getMessageId()
                    : null;

            Long unreadCount = messageRepository.countUnreadMessages(chatRoomId, lastReadMessageId, chatPart.getEnterChatRoomAt());

            ChatRoomResponse response = ChatRoomResponse.from(
                    chatPart.getChatRoom(),
                    participantsMap.get(chatRoomId),
                    unreadCount
            );

            responses.add(response);
        }

        return responses;
    }
}
