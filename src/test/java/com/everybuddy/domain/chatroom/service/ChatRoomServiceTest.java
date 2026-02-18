package com.everybuddy.domain.chatroom.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.dto.ChatRoomResponse;
import com.everybuddy.domain.chatroom.dto.CreateChatRoomRequest;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 단위 테스트")
class ChatRoomServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatPartRepository chatPartRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private FirebaseDatabase firebaseDatabase;
    @Mock private DatabaseReference databaseReference;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private User creator;
    private User participant1;
    private User participant2;
    private User deletedUser;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        creator = User.createForTest(1L, "creator", "생성자", "password",
                Country.KOREA, Language.KOREAN, Gender.MALE, LocalDate.of(1990, 1, 1));

        participant1 = User.createForTest(2L, "participant1", "참여자1", "password",
                Country.KOREA, Language.KOREAN, Gender.FEMALE, LocalDate.of(1995, 1, 1));

        participant2 = User.createForTest(3L, "participant2", "참여자2", "password",
                Country.KOREA, Language.KOREAN, Gender.MALE, LocalDate.of(2000, 1, 1));

        deletedUser = User.createForTest(4L, "deleted", "삭제된유저", "password",
                Country.KOREA, Language.KOREAN, Gender.FEMALE, LocalDate.of(1992, 1, 1));
        deletedUser.softDelete();

        chatRoom = ChatRoom.createForTest(1L, "테스트 채팅방");
    }

    // ===== 헬퍼 메서드 =====

    private void setupFirebaseMock() {
        when(firebaseDatabase.getReference(anyString())).thenReturn(databaseReference);
        when(databaseReference.child(anyString())).thenReturn(databaseReference);
        when(databaseReference.setValueAsync(any())).thenReturn(null);
    }

    /**
     * creator 검증 통과 후 참여자 검증에서 예외 발생하는 케이스를 위한 Mock 설정
     * (chatRoom 저장, creator ChatPart 저장까지 진행된 후 예외 발생)
     */
    private void setupMocksForParticipantException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPartRepository.save(any(ChatPart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void verifyFirebaseParticipants(Long... expectedIds) {
        ArgumentCaptor<Map<String, Boolean>> captor = ArgumentCaptor.forClass(Map.class);
        verify(firebaseDatabase).getReference("chatrooms");
        verify(databaseReference).setValueAsync(captor.capture());

        Map<String, Boolean> savedMap = captor.getValue();
        assertEquals(expectedIds.length, savedMap.size());
        for (Long id : expectedIds) {
            assertTrue(savedMap.containsKey(String.valueOf(id)));
        }
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
            when(chatPartRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
            setupFirebaseMock();
        }

        @Test
        @DisplayName("TC-1-1. participantIds에 1명 포함 (최소 케이스)")
        void createChatRoomWithOneParticipant() {
            // given
            when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(participant1));

            // when
            ChatRoomResponse response = chatRoomService.createChatRoom(1L,
                    CreateChatRoomRequest.of("테스트방", List.of(2L)));

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
            ArgumentCaptor<List<ChatPart>> partsCaptor = ArgumentCaptor.forClass(List.class);
            verify(chatPartRepository).saveAll(partsCaptor.capture());
            List<ChatPart> savedParts = partsCaptor.getValue();
            assertEquals(1, savedParts.size());
            assertEquals(2L, savedParts.get(0).getUser().getUserId());

            verifyFirebaseParticipants(1L, 2L);
        }

        @Test
        @DisplayName("TC-1-2. participantIds에 여러 명 포함")
        void createChatRoomWithMultipleParticipants() {
            // given
            when(userRepository.findAllById(List.of(2L, 3L))).thenReturn(List.of(participant1, participant2));

            // when
            ChatRoomResponse response = chatRoomService.createChatRoom(1L,
                    CreateChatRoomRequest.of("테스트방", List.of(2L, 3L)));

            // then
            assertAll(
                    () -> assertEquals(1L, response.getChatRoomId()),
                    () -> assertEquals("테스트방", response.getRoomName()),
                    () -> assertEquals(3, response.getParticipantIds().size()),
                    () -> assertTrue(response.getParticipantIds().containsAll(List.of(1L, 2L, 3L)))
            );

            // 다른 참여자들 저장 값 검증
            ArgumentCaptor<List<ChatPart>> partsCaptor = ArgumentCaptor.forClass(List.class);
            verify(chatPartRepository).saveAll(partsCaptor.capture());
            List<ChatPart> savedParts = partsCaptor.getValue();
            assertEquals(2, savedParts.size());
            assertTrue(savedParts.stream().anyMatch(cp -> cp.getUser().getUserId().equals(2L)));
            assertTrue(savedParts.stream().anyMatch(cp -> cp.getUser().getUserId().equals(3L)));

            verifyFirebaseParticipants(1L, 2L, 3L);
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

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.createChatRoom(999L,
                            CreateChatRoomRequest.of("테스트방", List.of(2L))));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-2. participantIds에 존재하지 않는 유저 포함")
        void participantNotFound() {
            // given
            setupMocksForParticipantException();
            when(userRepository.findAllById(List.of(2L, 999L))).thenReturn(List.of(participant1));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.createChatRoom(1L,
                            CreateChatRoomRequest.of("테스트방", List.of(2L, 999L))));

            assertEquals(ErrorCode.PARTICIPANT_NOT_FOUND, ex.getErrorCode());
            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(chatPartRepository, never()).saveAll(any());
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

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.createChatRoom(4L,
                            CreateChatRoomRequest.of("테스트방", List.of(2L))));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-3-2. participantIds에 삭제된 유저 포함")
        void deletedUserInParticipants() {
            // given
            setupMocksForParticipantException();
            when(userRepository.findAllById(List.of(2L, 4L))).thenReturn(List.of(participant1, deletedUser));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> chatRoomService.createChatRoom(1L,
                            CreateChatRoomRequest.of("테스트방", List.of(2L, 4L))));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(chatPartRepository, never()).saveAll(any());
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
            verify(messageRepository, never()).countUnreadMessages(any(), any());
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
            when(messageRepository.countUnreadMessages(1L, null)).thenReturn(5L);

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
            verify(messageRepository).countUnreadMessages(1L, null);
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
            when(messageRepository.countUnreadMessages(1L, null)).thenReturn(3L);
            when(messageRepository.countUnreadMessages(2L, null)).thenReturn(7L);
            when(messageRepository.countUnreadMessages(3L, null)).thenReturn(0L);

            // when
            List<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(1L);

            // then
            assertAll(
                    () -> assertEquals(3, responses.size()),
                    () -> assertEquals(3L, responses.get(0).getUnreadCount()),
                    () -> assertEquals(7L, responses.get(1).getUnreadCount()),
                    () -> assertEquals(0L, responses.get(2).getUnreadCount())
            );
            verify(messageRepository, times(3)).countUnreadMessages(any(), any());
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
            when(messageRepository.countUnreadMessages(1L, 10L)).thenReturn(3L);

            // when
            List<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(1L);

            // then
            assertAll(
                    () -> assertEquals(1, responses.size()),
                    () -> assertEquals(3L, responses.get(0).getUnreadCount())
            );
            verify(messageRepository).countUnreadMessages(1L, 10L);
        }
    }
}
