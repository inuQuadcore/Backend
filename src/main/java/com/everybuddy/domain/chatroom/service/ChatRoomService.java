package com.everybuddy.domain.chatroom.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.dto.ChatRoomResponse;
import com.everybuddy.domain.chatroom.dto.CreateChatRoomRequest;
import com.everybuddy.domain.chatroom.dto.InviteMembersRequest;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.event.ChatRoomCreatedEvent;
import com.everybuddy.domain.chatroom.event.ChatRoomLeftEvent;
import com.everybuddy.domain.chatroom.event.ChatRoomMembersInvitedEvent;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatPartRepository chatPartRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final BlockRelationRepository blockRelationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatRoomResponse createChatRoom(Long creatorId, CreateChatRoomRequest request) {
        // 1. 검증
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (creator.isDeleted()) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }

        boolean isGroup = request.getIsGroup();
        validateParticipantsSizeByType(isGroup, request.getParticipantIds());
        List<User> participants = validateParticipants(request.getParticipantIds());

        // 2. 1:1방 idempotent: 본인-상대 양쪽 모두 active인 1:1방이 있으면 그 방 반환
        if (!isGroup) {
            Long otherUserId = request.getParticipantIds().get(0);
            Optional<ChatRoom> existing = chatRoomRepository
                    .findActiveDirectChatRooms(creatorId, otherUserId).stream().findFirst();
            if (existing.isPresent()) {
                return ChatRoomResponse.from(existing.get(), List.of(creatorId, otherUserId));
            }
        }

        // 3. 저장
        ChatRoom chatRoom = ChatRoom.create(request.getRoomName(), isGroup);
        chatRoom = chatRoomRepository.save(chatRoom);

        List<Long> allParticipantIds = saveAllParticipants(creator, chatRoom, participants);

        // 4. Firebase: 커밋 성공 후 동기화
        eventPublisher.publishEvent(ChatRoomCreatedEvent.of(chatRoom.getChatRoomId(), allParticipantIds));

        return ChatRoomResponse.from(chatRoom, allParticipantIds);
    }

    @Transactional
    public void inviteMembers(Long inviterId, Long chatRoomId, InviteMembersRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));
        if (chatRoom.isDeleted()) {
            throw new CustomException(ErrorCode.CHATROOM_DELETED);
        }
        if (!chatRoom.isGroup()) {
            throw new CustomException(ErrorCode.CANNOT_INVITE_TO_DIRECT);
        }

        if (!chatPartRepository.existsByUserIdAndChatRoomId(inviterId, chatRoomId)) {
            throw new CustomException(ErrorCode.USER_NOT_IN_CHATROOM);
        }

        List<Long> invitedUserIds = new ArrayList<>();
        for (Long targetId : request.getParticipantIds()) {
            if (targetId.equals(inviterId)) {
                throw new CustomException(ErrorCode.CANNOT_INVITE_SELF);
            }

            User target = userRepository.findById(targetId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            if (target.isDeleted()) {
                throw new CustomException(ErrorCode.USER_DELETED);
            }

            if (blockRelationRepository.existsBlockRelationBetween(inviterId, targetId)) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }

            chatPartRepository.findAnyByUserIdAndChatRoomId(targetId, chatRoomId)
                    .ifPresentOrElse(
                            existing -> {
                                if (existing.isActive()) {
                                    throw new CustomException(ErrorCode.ALREADY_IN_CHATROOM);
                                }
                                existing.rejoin();
                            },
                            () -> chatPartRepository.save(ChatPart.create(target, chatRoom))
                    );
            invitedUserIds.add(targetId);
        }

        eventPublisher.publishEvent(ChatRoomMembersInvitedEvent.of(chatRoomId, invitedUserIds));
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

    private void validateParticipantsSizeByType(boolean isGroup, List<Long> participantIds) {
        if (!isGroup && participantIds.size() != 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
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
