package com.everybuddy.domain.message.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.repository.MediaRepository;
import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 단위 테스트")
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatPartRepository chatPartRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private StorageService storageService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MessageService messageService;

    private User testUser;
    private ChatRoom testChatRoom;
    private ChatMessageRequest textRequest;

    @BeforeEach
    void setUp() {
        testUser = User.createForTest(1L, "testuser", "테스트유저", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));

        testChatRoom = ChatRoom.createForTest(1L, "테스트 채팅방");

        textRequest = ChatMessageRequest.of(1L, "안녕하세요");
    }

    // ===== 테스트 헬퍼 메서드 =====

    private void setupCommonMocksForSendSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

        // MessageService가 save 반환값의 ID, sendAt을 사용하므로 채워진 객체 반환
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message input = invocation.getArgument(0);
            if (input.getMessageType() == MessageType.FILE) {
                return Message.createWithMediaForTest(1L, input.getChatRoom(), input.getUser(),
                        input.getMedia(), input.getMessageType(), LocalDateTime.now());
            } else {
                return Message.createForTest(1L, input.getChatRoom(), input.getUser(),
                        input.getMessageType(), input.getContent(), LocalDateTime.now());
            }
        });
    }

    private void setupFileUploadMocks(String fileName) {
        String s3Key = "s3/key/" + fileName;
        when(storageService.uploadChatFile(eq(1L), any(MultipartFile.class))).thenReturn(s3Key);
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("1. sendMessage() - 정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("TC-1-1. 텍스트 메시지 전송")
        void sendTextMessageOnly() {
            // given
            setupCommonMocksForSendSuccess();
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            // when
            messageService.sendMessage(1L, textRequest, null);

            // then
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertAll(
                    () -> assertEquals(MessageType.TEXT, savedMessage.getMessageType()),
                    () -> assertEquals("안녕하세요", savedMessage.getContent()),
                    () -> assertEquals(testUser, savedMessage.getUser()),
                    () -> assertEquals(testChatRoom, savedMessage.getChatRoom()),
                    () -> assertNull(savedMessage.getMedia())
            );
            verify(storageService, never()).uploadChatFile(any(), any());
        }

        @Test
        @DisplayName("TC-1-2. 파일 메시지 전송 (content = null)")
        void sendFileMessageOnly_contentNull() {
            // given
            setupCommonMocksForSendSuccess();
            setupFileUploadMocks("test.jpg");

            ChatMessageRequest fileRequest = ChatMessageRequest.of(1L, null);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", "test content".getBytes()
            );

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);

            // when
            messageService.sendMessage(1L, fileRequest, file);

            // then
            verify(storageService).uploadChatFile(eq(1L), any(MultipartFile.class));
            verify(mediaRepository).save(mediaCaptor.capture());
            verify(messageRepository).save(messageCaptor.capture());

            Media savedMedia = mediaCaptor.getValue();
            Message savedMessage = messageCaptor.getValue();

            assertAll(
                    () -> assertEquals(MessageType.FILE, savedMessage.getMessageType()),
                    () -> assertNull(savedMessage.getContent()),
                    () -> assertEquals(testUser, savedMessage.getUser()),
                    () -> assertEquals(testChatRoom, savedMessage.getChatRoom()),
                    () -> assertNotNull(savedMessage.getMedia()),
                    () -> assertEquals("test.jpg", savedMedia.getOriginalFilename())
            );
        }

        @Test
        @DisplayName("TC-1-3. 파일 메시지 전송 (content = 빈 문자열)")
        void sendFileMessageOnly_contentEmpty() {
            // given
            setupCommonMocksForSendSuccess();
            setupFileUploadMocks("test.jpg");

            ChatMessageRequest fileRequest = ChatMessageRequest.of(1L, "");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", "test content".getBytes()
            );

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            // when
            messageService.sendMessage(1L, fileRequest, file);

            // then
            verify(storageService).uploadChatFile(eq(1L), any(MultipartFile.class));
            verify(messageRepository).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();
            assertAll(
                    () -> assertEquals(MessageType.FILE, savedMessage.getMessageType()),
                    () -> assertNull(savedMessage.getContent()),
                    () -> assertNotNull(savedMessage.getMedia())
            );
        }
    }

    @Nested
    @DisplayName("2. sendMessage() - 엔티티 조회 실패")
    class EntityNotFoundCases {

        @Test
        @DisplayName("TC-2-1. 존재하지 않는 사용자")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.sendMessage(999L, textRequest, null));

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-2. 존재하지 않는 채팅방")
        void chatRoomNotFound() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(999L, "안녕하세요");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.sendMessage(1L, request, null));

            assertEquals(ErrorCode.CHATROOM_NOT_FOUND, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-3. 채팅방 미참여 사용자")
        void userNotInChatRoom() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(false);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.sendMessage(1L, textRequest, null));

            assertEquals(ErrorCode.USER_NOT_IN_CHATROOM, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("3. sendMessage() - 입력값 검증 실패")
    class ValidationFailureCases {

        @Test
        @DisplayName("TC-3-1. 파일과 텍스트 동시 전송")
        void cannotSendFileAndTextTogether() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(1L, "파일 설명");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", "test content".getBytes()
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.sendMessage(1L, request, file));

            assertEquals(ErrorCode.CANNOT_SEND_FILE_AND_TEXT_TOGETHER, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
            verify(storageService, never()).uploadChatFile(any(), any());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("TC-3-2/3-3/3-4. TEXT 타입인데 content가 null/빈 문자열/공백")
        void textMessageWithBlankOrNullContent(String content) {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);
            ChatMessageRequest request = ChatMessageRequest.of(1L, content);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.sendMessage(1L, request, null));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-3-5. 크기 0인 파일은 파일 없음으로 판단하여 content 검증")
        void emptyFileIsConsideredNoFile() {
            // given: 크기 0인 파일 → isEmpty() = true → hasFile = false → TEXT 타입으로 판단
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", new byte[0]
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then
            ChatMessageRequest request = ChatMessageRequest.of(1L, null);
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.sendMessage(1L, request, emptyFile));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
            verify(storageService, never()).uploadChatFile(any(), any());
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("4. sendMessage() - Soft Delete 검증")
    class SoftDeleteValidationCases {

        @Test
        @DisplayName("TC-4-1. 삭제된 유저는 메시지 전송 불가")
        void deletedUserCannotSendMessage() {
            // given
            testUser.softDelete();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.sendMessage(1L, textRequest, null));

            assertEquals(ErrorCode.USER_DELETED, exception.getErrorCode());
            verify(chatRoomRepository, never()).findById(any());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-4-2. 삭제된 채팅방에는 메시지 전송 불가")
        void cannotSendMessageToDeletedChatRoom() {
            // given
            testChatRoom.softDelete();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.sendMessage(1L, textRequest, null));

            assertEquals(ErrorCode.CHATROOM_DELETED, exception.getErrorCode());
            verify(chatPartRepository, never()).existsByUserIdAndChatRoomId(any(), any());
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("5. deleteMessage() 테스트")
    class DeleteMessageCases {

        @Test
        @DisplayName("TC-5-1. 메시지 없음")
        void messageNotFound() {
            // given
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());
            long userId = testUser.getUserId();

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.deleteMessage(userId, 999L));

            assertEquals(ErrorCode.MESSAGE_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("TC-5-2. 다른 사람의 메시지 삭제 시도")
        void cannotDeleteOtherUserMessage() {
            // given
            User otherUser = User.createForTest(2L, "otheruser", "다른유저", "password",
                    Country.KOREA, Gender.FEMALE, LocalDate.of(1995, 1, 1));
            Message message = Message.createForTest(1L, testChatRoom, otherUser, MessageType.TEXT,
                    "다른 사람 메시지", LocalDateTime.now());

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            long userId = testUser.getUserId();

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.deleteMessage(userId, 1L));

            assertEquals(ErrorCode.NOT_MESSAGE_OF_USER, exception.getErrorCode());
            assertFalse(message.isDeleted());
        }

        @Test
        @DisplayName("TC-5-3. 이미 삭제된 메시지 재삭제 시도")
        void cannotDeleteAlreadyDeletedMessage() {
            // given
            Message message = Message.createForTest(1L, testChatRoom, testUser, MessageType.TEXT,
                    "이미 삭제된 메시지", LocalDateTime.now());
            message.softDelete();

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            long userId = testUser.getUserId();

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.deleteMessage(userId, 1L));

            assertEquals(ErrorCode.MESSAGE_ALREADY_DELETED, exception.getErrorCode());
        }

        @Test
        @DisplayName("TC-5-4. 마지막 메시지 삭제 성공 (soft delete 확인)")
        void deleteLastMessageSuccess() {
            // given: 삭제 대상 메시지가 채팅방의 마지막 메시지
            Message message = Message.createForTest(1L, testChatRoom, testUser, MessageType.TEXT,
                    "삭제할 메시지", LocalDateTime.now());

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            when(messageRepository.findLastMessageId(testChatRoom)).thenReturn(Optional.of(1L));
            when(chatPartRepository.findByChatRoomIdWithUser(testChatRoom.getChatRoomId()))
                    .thenReturn(List.of(ChatPart.create(testUser, testChatRoom)));

            // when
            messageService.deleteMessage(testUser.getUserId(), 1L);

            // then
            assertTrue(message.isDeleted());
            verify(chatPartRepository).findByChatRoomIdWithUser(testChatRoom.getChatRoomId());
        }

        @Test
        @DisplayName("TC-5-5. 마지막이 아닌 메시지 삭제 시 참여자 조회 생략")
        void deleteNonLastMessageSkipsParticipantQuery() {
            // given: 삭제 대상(id=1)이 마지막 메시지(id=2)가 아님 → isLast = false
            Message message = Message.createForTest(1L, testChatRoom, testUser, MessageType.TEXT,
                    "삭제할 메시지", LocalDateTime.now());

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            when(messageRepository.findLastMessageId(testChatRoom)).thenReturn(Optional.of(2L));

            // when
            messageService.deleteMessage(testUser.getUserId(), 1L);

            // then
            assertTrue(message.isDeleted());
            verify(chatPartRepository, never()).findByChatRoomIdWithUser(any());
        }

        @Test
        @DisplayName("TC-5-6. 파일 메시지 삭제 시 Media도 soft delete")
        void deleteFileMessageAlsoDeletesMedia() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", "content".getBytes()
            );
            Media media = Media.from(testUser, testChatRoom, "s3-key", file);
            Message message = Message.createWithMediaForTest(1L, testChatRoom, testUser,
                    media, MessageType.FILE, LocalDateTime.now());

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            when(messageRepository.findLastMessageId(testChatRoom)).thenReturn(Optional.of(1L));
            when(chatPartRepository.findByChatRoomIdWithUser(testChatRoom.getChatRoomId()))
                    .thenReturn(List.of(ChatPart.create(testUser, testChatRoom)));

            // when
            messageService.deleteMessage(testUser.getUserId(), 1L);

            // then
            assertTrue(message.isDeleted());
            assertTrue(message.getMedia().isDeleted());
            verify(chatPartRepository).findByChatRoomIdWithUser(testChatRoom.getChatRoomId());
        }
    }

    @Nested
    @DisplayName("6. markAsRead() 테스트")
    class MarkAsReadCases {

        @Test
        @DisplayName("TC-6-1. 메시지 읽음 처리 성공")
        void markMessageAsReadSuccess() {
            // given
            Message message = Message.createForTest(1L, testChatRoom, testUser, MessageType.TEXT,
                    "읽을 메시지", LocalDateTime.now());
            ChatPart chatPart = ChatPart.create(testUser, testChatRoom);

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            when(chatPartRepository.findByUserIdAndChatRoomId(testUser.getUserId(), 1L))
                    .thenReturn(Optional.of(chatPart));

            // when
            messageService.markAsRead(testUser.getUserId(), 1L);

            // then: 마지막 읽은 메시지가 실제로 업데이트됐는지 확인
            assertEquals(message, chatPart.getLastReadMessage());
        }

        @Test
        @DisplayName("TC-6-2. 채팅방 미참여 유저의 읽음 처리 시도")
        void cannotMarkAsReadByNonParticipant() {
            // given
            Message message = Message.createForTest(1L, testChatRoom, testUser, MessageType.TEXT,
                    "읽을 메시지", LocalDateTime.now());

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            when(chatPartRepository.findByUserIdAndChatRoomId(999L, 1L))
                    .thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> messageService.markAsRead(999L, 1L));

            assertEquals(ErrorCode.USER_NOT_IN_CHATROOM, exception.getErrorCode());
        }
    }
}
