package com.everybuddy.domain.chatroom.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.dto.ChatRoomResponse;
import com.everybuddy.domain.chatroom.dto.CreateChatRoomRequest;
import com.everybuddy.domain.chatroom.dto.InviteMembersRequest;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.event.ChatRoomLeftEvent;
import com.everybuddy.domain.chatroom.event.ChatRoomMembersInvitedEvent;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 단위 테스트")
class ChatRoomServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatPartRepository chatPartRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private BlockRelationRepository blockRelationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Captor
    private ArgumentCaptor<List<ChatPart>> chatPartsCaptor;

    private User creator;
    private User participant1;
    private User participant2;
    private User deletedUser;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        creator = User.createForTest(1L, "creator", "생성자", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));

        participant1 = User.createForTest(2L, "participant1", "참여자1", "password",
                Country.KOREA, Gender.FEMALE, LocalDate.of(1995, 1, 1));

        participant2 = User.createForTest(3L, "participant2", "참여자2", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(2000, 1, 1));

        deletedUser = User.createForTest(4L, "deleted", "삭제된유저", "password",
                Country.KOREA, Gender.FEMALE, LocalDate.of(1992, 1, 1));
        deletedUser.softDelete();

        chatRoom = ChatRoom.createForTest(1L, "테스트 채팅방");
    }

    @Nested
    @DisplayName("1. createChatRoom() - 정상 케이스")
    class CreateChatRoomSuccessCases {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
                ChatRoom input = invocation.getArgument(0);
                return ChatRoom.createForTest(1L, input.getRoomName());
            });
            when(chatPartRepository.save(any(ChatPart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("TC-1-1. participantIds에 1명 포함 (최소 케이스)")
        void createChatRoomWithOneParticipant() {
            // given
            when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(participant1));
            when(chatPartRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoomResponse response = chatRoomService.createChatRoom(1L,
                    CreateChatRoomRequest.ofForTest("테스트방", List.of(2L)));

            // then
            assertAll(
                    () -> assertEquals(1L, response.getChatRoomId()),
                    () -> assertEquals("테스트방", response.getRoomName()),
                    () -> assertEquals(2, response.getParticipantIds().size()),
                    () -> assertTrue(response.getParticipantIds().containsAll(List.of(1L, 2L)))
            );

            // ChatRoom 저장 값 검증
            ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(chatRoomCaptor.capture());
            assertEquals("테스트방", chatRoomCaptor.getValue().getRoomName());

            // ChatPart (creator) 저장 값 검증
            ArgumentCaptor<ChatPart> chatPartCaptor = ArgumentCaptor.forClass(ChatPart.class);
            verify(chatPartRepository).save(chatPartCaptor.capture());
            assertEquals(1L, chatPartCaptor.getValue().getUser().getUserId());

            // 다른 참여자 저장 값 검증
            verify(chatPartRepository).saveAll(chatPartsCaptor.capture());
            List<ChatPart> savedParts = chatPartsCaptor.getValue();
            assertEquals(1, savedParts.size());
            assertEquals(2L, savedParts.get(0).getUser().getUserId());
        }

        @Test
        @DisplayName("TC-1-2. participantIds가 빈 리스트 (생성자만 참여)")
        void createChatRoomWithNoOtherParticipants() {
            // given: participantIds가 빈 리스트 → validateParticipants가 바로 List.of() 반환
            // when
            ChatRoomResponse response = chatRoomService.createChatRoom(1L,
                    CreateChatRoomRequest.ofForTest("테스트방", List.of()));

            // then
            assertAll(
                    () -> assertEquals(1, response.getParticipantIds().size()),
                    () -> assertTrue(response.getParticipantIds().contains(1L))
            );
            ArgumentCaptor<ChatPart> chatPartCaptor = ArgumentCaptor.forClass(ChatPart.class);
            verify(chatPartRepository).save(chatPartCaptor.capture());
            assertEquals(1L, chatPartCaptor.getValue().getUser().getUserId());
            verify(userRepository, never()).findAllById(any());
            verify(chatPartRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("TC-1-3. participantIds에 여러 명 포함")
        void createChatRoomWithMultipleParticipants() {
            // given
            when(userRepository.findAllById(List.of(2L, 3L))).thenReturn(List.of(participant1, participant2));
            when(chatPartRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoomResponse response = chatRoomService.createChatRoom(1L,
                    CreateChatRoomRequest.ofForTest("테스트방", List.of(2L, 3L)));

            // then
            assertAll(
                    () -> assertEquals(1L, response.getChatRoomId()),
                    () -> assertEquals("테스트방", response.getRoomName()),
                    () -> assertEquals(3, response.getParticipantIds().size()),
                    () -> assertTrue(response.getParticipantIds().containsAll(List.of(1L, 2L, 3L)))
            );

            // 다른 참여자들 저장 값 검증
            verify(chatPartRepository).saveAll(chatPartsCaptor.capture());
            List<ChatPart> savedParts = chatPartsCaptor.getValue();
            assertEquals(2, savedParts.size());
            assertTrue(savedParts.stream().anyMatch(cp -> cp.getUser().getUserId().equals(2L)));
            assertTrue(savedParts.stream().anyMatch(cp -> cp.getUser().getUserId().equals(3L)));
        }
    }

    @Nested
    @DisplayName("2. createChatRoom() - 엔티티 조회 실패")
    class CreateChatRoomEntityNotFoundCases {

        @Test
        @DisplayName("TC-2-1. 존재하지 않는 creatorId")
        void creatorNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            CreateChatRoomRequest request = CreateChatRoomRequest.ofForTest("테스트방", List.of(2L));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.createChatRoom(999L, request));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-2. participantIds에 존재하지 않는 유저 포함")
        void participantNotFound() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(userRepository.findAllById(List.of(2L, 999L))).thenReturn(List.of(participant1));
            CreateChatRoomRequest request = CreateChatRoomRequest.ofForTest("테스트방", List.of(2L, 999L));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.createChatRoom(1L, request));

            assertEquals(ErrorCode.PARTICIPANT_NOT_FOUND, ex.getErrorCode());
            verify(chatRoomRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("3. createChatRoom() - Soft Delete 검증")
    class CreateChatRoomSoftDeleteCases {

        @Test
        @DisplayName("TC-3-1. 삭제된 유저가 채팅방 생성 시도")
        void deletedUserCannotCreateChatRoom() {
            // given
            when(userRepository.findById(4L)).thenReturn(Optional.of(deletedUser));
            CreateChatRoomRequest request = CreateChatRoomRequest.ofForTest("테스트방", List.of(2L));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.createChatRoom(4L, request));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-3-2. participantIds에 삭제된 유저 포함")
        void deletedUserInParticipants() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(userRepository.findAllById(List.of(2L, 4L))).thenReturn(List.of(participant1, deletedUser));
            CreateChatRoomRequest request = CreateChatRoomRequest.ofForTest("테스트방", List.of(2L, 4L));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.createChatRoom(1L, request));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(chatRoomRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("4. getMyChatRooms() - 정상 케이스")
    class GetMyChatRoomsSuccessCases {

        @Test
        @DisplayName("TC-4-1. 참여 중인 채팅방이 없는 경우")
        void noChatRooms() {
            // given
            when(chatPartRepository.findByUserIdWithChatRoom(1L)).thenReturn(List.of());

            // when
            List<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(1L);

            // then
            assertTrue(responses.isEmpty());
            verify(chatPartRepository, never()).findByChatRoomIdsWithUser(any());
            verify(messageRepository, never()).countUnreadMessages(any(), any(), any());
        }

        @Test
        @DisplayName("TC-4-2. 참여 중인 채팅방이 1개 (lastReadMessage null)")
        void oneChatRoom() {
            // given
            ChatPart chatPart = ChatPart.create(creator, chatRoom);
            when(chatPartRepository.findByUserIdWithChatRoom(1L)).thenReturn(List.of(chatPart));
            when(chatPartRepository.findByChatRoomIdsWithUser(List.of(1L)))
                    .thenReturn(List.of(
                            ChatPart.create(creator, chatRoom),
                            ChatPart.create(participant1, chatRoom)));
            when(messageRepository.countUnreadMessages(eq(1L), isNull(), any(LocalDateTime.class))).thenReturn(5L);

            // when
            List<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(1L);

            // then
            assertAll(
                    () -> assertEquals(1, responses.size()),
                    () -> assertEquals(1L, responses.get(0).getChatRoomId()),
                    () -> assertEquals("테스트 채팅방", responses.get(0).getRoomName()),
                    () -> assertEquals(2, responses.get(0).getParticipantIds().size()),
                    () -> assertEquals(5L, responses.get(0).getUnreadCount())
            );
            verify(messageRepository).countUnreadMessages(eq(1L), isNull(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("TC-4-3. 참여 중인 채팅방이 여러 개")
        void multipleChatRooms() {
            // given
            ChatRoom chatRoom2 = ChatRoom.createForTest(2L, "채팅방2");
            ChatRoom chatRoom3 = ChatRoom.createForTest(3L, "채팅방3");

            when(chatPartRepository.findByUserIdWithChatRoom(1L))
                    .thenReturn(List.of(
                            ChatPart.create(creator, chatRoom),
                            ChatPart.create(creator, chatRoom2),
                            ChatPart.create(creator, chatRoom3)));
            when(chatPartRepository.findByChatRoomIdsWithUser(List.of(1L, 2L, 3L)))
                    .thenReturn(List.of(
                            ChatPart.create(creator, chatRoom),
                            ChatPart.create(participant1, chatRoom),
                            ChatPart.create(creator, chatRoom2),
                            ChatPart.create(participant2, chatRoom2),
                            ChatPart.create(creator, chatRoom3)));
            when(messageRepository.countUnreadMessages(eq(1L), isNull(), any(LocalDateTime.class))).thenReturn(3L);
            when(messageRepository.countUnreadMessages(eq(2L), isNull(), any(LocalDateTime.class))).thenReturn(7L);
            when(messageRepository.countUnreadMessages(eq(3L), isNull(), any(LocalDateTime.class))).thenReturn(0L);

            // when
            List<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(1L);

            // then
            assertAll(
                    () -> assertEquals(3, responses.size()),
                    () -> assertEquals(3L, responses.get(0).getUnreadCount()),
                    () -> assertEquals(7L, responses.get(1).getUnreadCount()),
                    () -> assertEquals(0L, responses.get(2).getUnreadCount())
            );
            verify(messageRepository).countUnreadMessages(eq(1L), isNull(), any(LocalDateTime.class));
            verify(messageRepository).countUnreadMessages(eq(2L), isNull(), any(LocalDateTime.class));
            verify(messageRepository).countUnreadMessages(eq(3L), isNull(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("TC-4-4. lastReadMessage가 있는 경우")
        void lastReadMessageExists() {
            // given
            Message lastMessage = Message.createForTest(10L, chatRoom, creator,
                    MessageType.TEXT, "마지막 읽은 메시지", LocalDateTime.now());
            ChatPart chatPart = ChatPart.create(creator, chatRoom);
            chatPart.updateLastReadMessage(lastMessage);

            when(chatPartRepository.findByUserIdWithChatRoom(1L)).thenReturn(List.of(chatPart));
            when(chatPartRepository.findByChatRoomIdsWithUser(List.of(1L)))
                    .thenReturn(List.of(ChatPart.create(creator, chatRoom)));
            when(messageRepository.countUnreadMessages(eq(1L), eq(10L), any(LocalDateTime.class))).thenReturn(3L);

            // when
            List<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(1L);

            // then
            assertAll(
                    () -> assertEquals(1, responses.size()),
                    () -> assertEquals(3L, responses.get(0).getUnreadCount())
            );
            verify(messageRepository).countUnreadMessages(eq(1L), eq(10L), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("3. leaveChatRoom() 테스트")
    class LeaveChatRoomCases {

        @Test
        @DisplayName("TC-3-1. 채팅방 나가기 성공 → ChatPart inactive + ChatRoomLeftEvent 발행")
        void leaveSuccess() {
            ChatPart chatPart = ChatPart.create(creator, chatRoom);
            when(chatPartRepository.findByUserIdAndChatRoomId(1L, 1L)).thenReturn(Optional.of(chatPart));

            chatRoomService.leaveChatRoom(1L, 1L);

            assertAll(
                    () -> assertFalse(chatPart.isActive()),
                    () -> assertNotNull(chatPart.getExitChatRoomAt())
            );
            ArgumentCaptor<ChatRoomLeftEvent> eventCaptor = ArgumentCaptor.forClass(ChatRoomLeftEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertAll(
                    () -> assertEquals(1L, eventCaptor.getValue().getChatRoomId()),
                    () -> assertEquals(1L, eventCaptor.getValue().getUserId())
            );
        }

        @Test
        @DisplayName("TC-3-2. 채팅방 참여자가 아님 → USER_NOT_IN_CHATROOM, 이벤트 미발행")
        void leaveFailNotParticipant() {
            when(chatPartRepository.findByUserIdAndChatRoomId(1L, 1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.leaveChatRoom(1L, 1L));

            assertEquals(ErrorCode.USER_NOT_IN_CHATROOM, ex.getErrorCode());
            verify(eventPublisher, never()).publishEvent(any(ChatRoomLeftEvent.class));
        }
    }

    @Nested
    @DisplayName("4. inviteMembers() 테스트")
    class InviteMembersCases {

        @Test
        @DisplayName("TC-4-1. 신규 멤버 초대 → 새 ChatPart 생성 + 이벤트 발행")
        void inviteNewMember() {
            InviteMembersRequest request = InviteMembersRequest.ofForTest(List.of(2L));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant1));
            when(blockRelationRepository.existsBlockRelationBetween(1L, 2L)).thenReturn(false);
            when(chatPartRepository.findAnyByUserIdAndChatRoomId(2L, 1L)).thenReturn(Optional.empty());

            chatRoomService.inviteMembers(1L, 1L, request);

            ArgumentCaptor<ChatPart> partCaptor = ArgumentCaptor.forClass(ChatPart.class);
            verify(chatPartRepository).save(partCaptor.capture());
            assertAll(
                    () -> assertEquals(2L, partCaptor.getValue().getUser().getUserId()),
                    () -> assertTrue(partCaptor.getValue().isActive())
            );
            ArgumentCaptor<ChatRoomMembersInvitedEvent> eventCaptor = ArgumentCaptor.forClass(ChatRoomMembersInvitedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertAll(
                    () -> assertEquals(1L, eventCaptor.getValue().getChatRoomId()),
                    () -> assertEquals(List.of(2L), eventCaptor.getValue().getInvitedUserIds())
            );
        }

        @Test
        @DisplayName("TC-4-2. 이전에 나간 멤버 재초대 → ChatPart rejoin (active=true, enterChatRoomAt 갱신)")
        void inviteRejoiningMember() {
            InviteMembersRequest request = InviteMembersRequest.ofForTest(List.of(2L));
            ChatPart inactivePart = ChatPart.create(participant1, chatRoom);
            inactivePart.leave();
            LocalDateTime beforeRejoin = inactivePart.getEnterChatRoomAt();

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant1));
            when(blockRelationRepository.existsBlockRelationBetween(1L, 2L)).thenReturn(false);
            when(chatPartRepository.findAnyByUserIdAndChatRoomId(2L, 1L)).thenReturn(Optional.of(inactivePart));

            chatRoomService.inviteMembers(1L, 1L, request);

            assertAll(
                    () -> assertTrue(inactivePart.isActive()),
                    () -> assertNull(inactivePart.getExitChatRoomAt()),
                    () -> assertTrue(inactivePart.getEnterChatRoomAt().isAfter(beforeRejoin)
                            || inactivePart.getEnterChatRoomAt().isEqual(beforeRejoin))
            );
            verify(chatPartRepository, never()).save(any(ChatPart.class));
            verify(eventPublisher).publishEvent(any(ChatRoomMembersInvitedEvent.class));
        }

        @Test
        @DisplayName("TC-4-3. 자기 자신 초대 → CANNOT_INVITE_SELF, 이벤트 미발행")
        void inviteSelfFails() {
            InviteMembersRequest request = InviteMembersRequest.ofForTest(List.of(1L));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.inviteMembers(1L, 1L, request));

            assertEquals(ErrorCode.CANNOT_INVITE_SELF, ex.getErrorCode());
            verify(eventPublisher, never()).publishEvent(any(ChatRoomMembersInvitedEvent.class));
        }

        @Test
        @DisplayName("TC-4-4. 초대자가 채팅방 참여자 아님 → USER_NOT_IN_CHATROOM")
        void inviterNotInChatRoom() {
            InviteMembersRequest request = InviteMembersRequest.ofForTest(List.of(2L));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.inviteMembers(1L, 1L, request));

            assertEquals(ErrorCode.USER_NOT_IN_CHATROOM, ex.getErrorCode());
            verify(eventPublisher, never()).publishEvent(any(ChatRoomMembersInvitedEvent.class));
        }

        @Test
        @DisplayName("TC-4-5. 이미 active 멤버 재초대 → ALREADY_IN_CHATROOM")
        void inviteAlreadyActiveMember() {
            InviteMembersRequest request = InviteMembersRequest.ofForTest(List.of(2L));
            ChatPart activePart = ChatPart.create(participant1, chatRoom);

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant1));
            when(blockRelationRepository.existsBlockRelationBetween(1L, 2L)).thenReturn(false);
            when(chatPartRepository.findAnyByUserIdAndChatRoomId(2L, 1L)).thenReturn(Optional.of(activePart));

            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.inviteMembers(1L, 1L, request));

            assertEquals(ErrorCode.ALREADY_IN_CHATROOM, ex.getErrorCode());
            verify(eventPublisher, never()).publishEvent(any(ChatRoomMembersInvitedEvent.class));
        }

        @Test
        @DisplayName("TC-4-6. 양방향 차단 관계 → USER_NOT_FOUND로 노출")
        void inviteBlockedUser() {
            InviteMembersRequest request = InviteMembersRequest.ofForTest(List.of(2L));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant1));
            when(blockRelationRepository.existsBlockRelationBetween(1L, 2L)).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.inviteMembers(1L, 1L, request));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(eventPublisher, never()).publishEvent(any(ChatRoomMembersInvitedEvent.class));
        }

        @Test
        @DisplayName("TC-4-7. 탈퇴한 유저 초대 → USER_DELETED")
        void inviteDeletedUser() {
            InviteMembersRequest request = InviteMembersRequest.ofForTest(List.of(4L));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);
            when(userRepository.findById(4L)).thenReturn(Optional.of(deletedUser));

            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.inviteMembers(1L, 1L, request));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(eventPublisher, never()).publishEvent(any(ChatRoomMembersInvitedEvent.class));
        }
    }
}
