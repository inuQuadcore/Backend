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
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 단위 테스트")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatPartRepository chatPartRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private FirebaseDatabase firebaseDatabase;

    @Mock
    private DatabaseReference databaseReference;

    @InjectMocks
    private MessageService messageService;

    private User testUser;
    private ChatRoom testChatRoom;
    private ChatMessageRequest textRequest;
    private ChatMessageRequest emptyRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 User 생성 (ID 포함)
        testUser = User.createForTest(1L, "testuser", "테스트유저", "password",
                Country.KOREA, Language.KOREAN, Gender.MALE, LocalDate.of(1990, 1, 1));

        // 테스트용 ChatRoom 생성 (ID 포함)
        testChatRoom = ChatRoom.createForTest(1L, "테스트 채팅방");

        // 텍스트 메시지 요청
        textRequest = ChatMessageRequest.of(1L, "안녕하세요");

        // 빈 요청
        emptyRequest = ChatMessageRequest.of(1L, null);
    }

    // ===== 테스트 헬퍼 메서드 =====

    /**
     * sendMessage 성공 케이스를 위한 공통 Mock 설정
     */
    private void setupCommonMocksForSendSuccess() {
        setupFirebaseMockForSend();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

        ChatPart chatPart = ChatPart.create(testUser, testChatRoom);
        when(chatPartRepository.findByChatRoomIdWithUser(1L)).thenReturn(List.of(chatPart));

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

    /**
     * 파일 업로드 관련 Mock 설정
     */
    private void setupFileUploadMocks(String fileName) {
        String s3Key = "s3/key/" + fileName;
        when(storageService.uploadChatFile(eq(1L), any(MultipartFile.class))).thenReturn(s3Key);
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Firebase Mock 설정 (sendMessage용 - setValueAsync + updateChildrenAsync)
     */
    private void setupFirebaseMockForSend() {
        when(firebaseDatabase.getReference(anyString())).thenReturn(databaseReference);
        when(databaseReference.child(anyString())).thenReturn(databaseReference);
        when(databaseReference.setValueAsync(any())).thenReturn(null);
        when(databaseReference.updateChildrenAsync(any())).thenReturn(null);
    }

    /**
     * Firebase Mock 설정 (deleteMessage용 - updateChildrenAsync만)
     */
    private void setupFirebaseMockForDelete() {
        when(firebaseDatabase.getReference(anyString())).thenReturn(databaseReference);
        when(databaseReference.child(anyString())).thenReturn(databaseReference);
        when(databaseReference.updateChildrenAsync(any())).thenReturn(null);
    }

    @Nested
    @DisplayName("1. 정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("TC-1-1. 텍스트 메시지만 전송")
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
        @DisplayName("TC-1-2. 파일 메시지만 전송 (content = null)")
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
        @DisplayName("TC-1-3. 파일 메시지만 전송 (content = 빈 문자열)")
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
    @DisplayName("2. 엔티티 조회 실패 케이스")
    class EntityNotFoundCases {

        @Test
        @DisplayName("TC-2-1. 존재하지 않는 사용자")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(999L, textRequest, null);
            });

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(userRepository, times(1)).findById(999L);
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
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, request, null);
            });

            assertEquals(ErrorCode.CHATROOM_NOT_FOUND, exception.getErrorCode());
            verify(chatRoomRepository, times(1)).findById(999L);
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-2-3. 채팅방 미참여 사용자")
        void userNotInChatRoom() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(false);  // 참여하지 않음

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, textRequest, null);
            });

            assertEquals(ErrorCode.USER_NOT_IN_CHATROOM, exception.getErrorCode());
            verify(chatPartRepository, times(1)).existsByUserIdAndChatRoomId(1L, 1L);
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("3. 검증 실패 케이스")
    class ValidationFailureCases {

        @Test
        @DisplayName("TC-3-1. 파일과 텍스트 동시 전송")
        void cannotSendFileAndTextTogether() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(1L, "파일 설명");

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    "test content".getBytes()
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, request, file);
            });

            assertEquals(ErrorCode.CANNOT_SEND_FILE_AND_TEXT_TOGETHER, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
            verify(storageService, never()).uploadChatFile(any(), any());
        }

        @Test
        @DisplayName("TC-3-2. TEXT 타입인데 content가 null")
        void textMessageWithNullContent() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(1L, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, request, null);
            });

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-3-3. TEXT 타입인데 content가 빈 문자열")
        void textMessageWithEmptyContent() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(1L, "");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, request, null);
            });

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-3-4. TEXT 타입인데 content가 공백만")
        void textMessageWithBlankContent() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(1L, "   ");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, request, null);
            });

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-3-5. FILE 타입인데 파일 크기가 0")
        void fileMessageWithZeroSize() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(1L, null);

            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    new byte[0]  // 크기 0
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then
            // 파일 크기 0 → hasFile = false → TEXT 타입으로 판단 → content null 검증
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, request, emptyFile);
            });

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
            verify(storageService, never()).uploadChatFile(any(), any());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-3-6. MultipartFile이 isEmpty() = true")
        void fileMessageWithEmptyFile() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(1L, null);

            // isEmpty()가 true인 파일 Mock
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then: TEXT 타입으로 판단되어 content 검증 진입
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, request, emptyFile);
            });

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
            verify(storageService, never()).uploadChatFile(any(), any());
        }
    }

    @Nested
    @DisplayName("4. Soft Delete 검증 케이스")
    class SoftDeleteValidationCases {

        @Test
        @DisplayName("TC-4-1. 삭제된 유저는 메시지 전송 불가")
        void deletedUserCannotSendMessage() {
            // given
            testUser.softDelete();  // 유저 삭제

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, textRequest, null);
            });

            assertEquals(ErrorCode.USER_DELETED, exception.getErrorCode());
            verify(chatRoomRepository, never()).findById(any());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-4-2. 삭제된 채팅방에는 메시지 전송 불가")
        void cannotSendMessageToDeletedChatRoom() {
            // given
            testChatRoom.softDelete();  // 채팅방 삭제

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, textRequest, null);
            });

            assertEquals(ErrorCode.CHATROOM_DELETED, exception.getErrorCode());
            verify(chatPartRepository, never()).existsByUserIdAndChatRoomId(any(), any());
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("5. deleteMessage() 테스트")
    class DeleteMessageCases {

        @Test
        @DisplayName("TC-5-1. 자신의 메시지 삭제 성공")
        void deleteOwnMessageSuccess() {
            // given
            setupFirebaseMockForDelete();

            Message message = Message.createForTest(1L, testChatRoom, testUser, MessageType.TEXT,
                    "삭제할 메시지", LocalDateTime.now());

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            when(messageRepository.findLastMessageId(any())).thenReturn(Optional.of(1L));

            ChatPart chatPart = ChatPart.create(testUser, testChatRoom);
            when(chatPartRepository.findByChatRoomIdWithUser(any())).thenReturn(java.util.List.of(chatPart));

            // when
            messageService.deleteMessage(testUser.getUserId(), 1L);

            // then
            assertTrue(message.isDeleted());
            verify(messageRepository).findById(1L);
        }

        @Test
        @DisplayName("TC-5-2. 다른 사람의 메시지 삭제 시도 시 예외")
        void cannotDeleteOtherUserMessage() {
            // given
            User otherUser = User.createForTest(2L, "otheruser", "다른유저", "password",
                    Country.KOREA, Language.KOREAN, Gender.FEMALE, LocalDate.of(1995, 1, 1));

            Message message = Message.createForTest(1L, testChatRoom, otherUser, MessageType.TEXT,
                    "다른 사람 메시지", LocalDateTime.now());

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.deleteMessage(testUser.getUserId(), 1L);
            });

            assertEquals(ErrorCode.NOT_MESSAGE_OF_USER, exception.getErrorCode());
            assertFalse(message.isDeleted());
        }

        @Test
        @DisplayName("TC-5-3. 이미 삭제된 메시지 재삭제 시도 시 예외")
        void cannotDeleteAlreadyDeletedMessage() {
            // given
            Message message = Message.createForTest(1L, testChatRoom, testUser, MessageType.TEXT,
                    "이미 삭제된 메시지", LocalDateTime.now());
            message.softDelete();  // 이미 삭제됨

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.deleteMessage(testUser.getUserId(), 1L);
            });

            assertEquals(ErrorCode.MESSAGE_ALREADY_DELETED, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("6. 경계값 테스트")
    class BoundaryTest {

        @Test
        @DisplayName("TC-6-1. 특수문자 포함 파일명")
        void sendFileWithSpecialCharactersInFilename() {
            // given
            setupCommonMocksForSendSuccess();
            String specialFileName = "파일명!@#$%.jpg";
            setupFileUploadMocks(specialFileName);

            ChatMessageRequest request = ChatMessageRequest.of(1L, null);
            MockMultipartFile file = new MockMultipartFile(
                    "file", specialFileName, "image/jpeg", "test content".getBytes()
            );

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            // when
            messageService.sendMessage(1L, request, file);

            // then
            verify(storageService).uploadChatFile(eq(1L), any(MultipartFile.class));
            verify(messageRepository).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();
            assertEquals(MessageType.FILE, savedMessage.getMessageType());
            assertNotNull(savedMessage.getMedia());
        }

        @Test
        @DisplayName("TC-6-2. content와 file 둘 다 null/없음")
        void sendMessageWithBothContentAndFileNull() {
            // given
            ChatMessageRequest request = ChatMessageRequest.of(1L, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatPartRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.sendMessage(1L, request, null);
            });

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("7. markAsRead() 테스트")
    class MarkAsReadCases {

        @Test
        @DisplayName("TC-7-1. 메시지 읽음 처리 성공")
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

            // then
            verify(messageRepository, times(1)).findById(1L);
            verify(chatPartRepository, times(1)).findByUserIdAndChatRoomId(testUser.getUserId(), 1L);
        }

        @Test
        @DisplayName("TC-7-2. 채팅방 미참여 유저의 읽음 처리 시도 시 예외")
        void cannotMarkAsReadByNonParticipant() {
            // given
            Message message = Message.createForTest(1L, testChatRoom, testUser, MessageType.TEXT,
                    "읽을 메시지", LocalDateTime.now());

            when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
            when(chatPartRepository.findByUserIdAndChatRoomId(999L, 1L))
                    .thenReturn(Optional.empty());  // 참여하지 않음

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                messageService.markAsRead(999L, 1L);
            });

            assertEquals(ErrorCode.USER_NOT_IN_CHATROOM, exception.getErrorCode());
        }
    }
}
