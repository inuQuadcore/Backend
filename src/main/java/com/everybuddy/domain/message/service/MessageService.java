package com.everybuddy.domain.message.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.repository.MediaRepository;
import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.message.dto.ChatRoomMetadata;
import com.everybuddy.domain.message.dto.UpdateMessageRequest;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.message.event.MessageDeletedEvent;
import com.everybuddy.domain.message.event.MessageReadEvent;
import com.everybuddy.domain.message.event.MessageSentEvent;
import com.everybuddy.domain.message.event.MessageUpdatedEvent;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.everybuddy.domain.message.dto.MessageResponse;
import com.everybuddy.domain.message.dto.MessageSyncResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatPartRepository chatPartRepository;
    private final MediaRepository mediaRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 메시지 전송 (파일이 있으면 FILE 메시지, 없으면 TEXT 메시지)
     */
    @Transactional
    public void sendMessage(Long userId, ChatMessageRequest request, MultipartFile file) {
        User user = findActiveUser(userId);
        ChatRoom chatRoom = findActiveChatRoom(request.getChatRoomId());
        validateParticipation(userId, chatRoom.getChatRoomId());

        boolean hasFile = file != null && !file.isEmpty();
        MessageType messageType = hasFile ? MessageType.FILE : MessageType.TEXT;
        validateMessageType(messageType, request.getContent(), file);

        // S3 업로드: DB 저장보다 먼저 수행
        // S3 실패 시 여기서 예외 발생 → DB 저장 전이므로 보상 처리 불필요
        String fileKey = hasFile ? storageService.uploadChatFile(chatRoom.getChatRoomId(), file) : null;

        // DB 저장: 실패 시 이미 업로드된 S3 파일 보상 삭제
        try {
            Message message = buildAndSaveMessage(user, chatRoom, messageType, request.getContent(), file, fileKey);
            eventPublisher.publishEvent(buildMessageSentEvent(message, chatRoom.getChatRoomId()));
        } catch (DataAccessException e) {
            deleteUploadedFileQuietly(fileKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public MessageSyncResponse getMessages(Long userId, Long chatRoomId, LocalDateTime since) {
        findActiveChatRoom(chatRoomId);
        ChatPart chatPart = chatPartRepository.findByUserIdAndChatRoomId(userId, chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_IN_CHATROOM));
        LocalDateTime enterChatRoomAt = chatPart.getEnterChatRoomAt();

        return MessageSyncResponse.of(
                resolveNewMessages(chatRoomId, since, enterChatRoomAt),
                resolveUpdatedMessages(chatRoomId, since, enterChatRoomAt),
                resolveDeletedIds(chatRoomId, since, enterChatRoomAt)
        );
    }

    @Transactional
    public MessageResponse updateMessage(Long userId, Long messageId, UpdateMessageRequest request) {
        Message message = findMessage(messageId);
        validateEditable(message, userId);

        message.update(request.getContent());
        eventPublisher.publishEvent(MessageUpdatedEvent.of(message, message.getChatRoom().getChatRoomId()));

        return MessageResponse.from(message, resolveFileUrl(message));
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        Message message = findMessage(messageId);
        validateDeletable(message, userId);

        message.softDelete();
        if (message.getMedia() != null) message.getMedia().softDelete();

        eventPublisher.publishEvent(buildMessageDeletedEvent(message));
    }

    @Transactional
    public void markAsRead(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        Long chatRoomId = message.getChatRoom().getChatRoomId();

        // 채팅방 참여자 확인 및 조회
        ChatPart chatPart = chatPartRepository.findByUserIdAndChatRoomId(userId, chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_IN_CHATROOM));

        // 마지막 읽은 메시지 업데이트
        chatPart.updateLastReadMessage(message);

        eventPublisher.publishEvent(MessageReadEvent.of(chatRoomId, userId));
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) throw new CustomException(ErrorCode.USER_DELETED);
        return user;
    }

    private ChatRoom findActiveChatRoom(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));
        if (chatRoom.isDeleted()) throw new CustomException(ErrorCode.CHATROOM_DELETED);
        return chatRoom;
    }

    private void validateParticipation(Long userId, Long chatRoomId) {
        if (!chatPartRepository.existsByUserIdAndChatRoomId(userId, chatRoomId)) {
            throw new CustomException(ErrorCode.USER_NOT_IN_CHATROOM);
        }
    }

    private Message findMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));
    }

    private void validateEditable(Message message, Long userId) {
        if (!message.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_MESSAGE_OF_USER);
        }
        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.MESSAGE_ALREADY_DELETED);
        }
        if (message.getMessageType() == MessageType.FILE) {
            throw new CustomException(ErrorCode.CANNOT_EDIT_FILE_MESSAGE);
        }
        if (message.getSendAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.MESSAGE_EDIT_TIME_EXCEEDED);
        }
    }

    private void validateDeletable(Message message, Long userId) {
        if (!message.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_MESSAGE_OF_USER);
        }
        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.MESSAGE_ALREADY_DELETED);
        }
        if (message.getSendAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.MESSAGE_EDIT_TIME_EXCEEDED);
        }
    }

    private MessageSentEvent buildMessageSentEvent(Message message, Long chatRoomId) {
        List<ChatPart> chatParts = chatPartRepository.findByChatRoomIdWithUser(chatRoomId);
        ChatRoomMetadata metadata = ChatRoomMetadata.from(message);
        return MessageSentEvent.of(message, chatRoomId, chatParts, metadata);
    }

    private MessageDeletedEvent buildMessageDeletedEvent(Message message) {
        Long chatRoomId = message.getChatRoom().getChatRoomId();
        Optional<Long> lastMessageId = messageRepository.findLastMessageId(message.getChatRoom());
        boolean isLast = isLastMessage(message.getMessageId(), lastMessageId);
        List<ChatPart> chatParts = isLast
                ? chatPartRepository.findByChatRoomIdWithUser(chatRoomId)
                : List.of();
        return MessageDeletedEvent.of(message, chatRoomId, isLast, chatParts);
    }

    /**
     * 메시지 타입에 따라 요청 데이터 검증
     */
    private void validateMessageType(MessageType messageType, String content, MultipartFile file) {
        // 파일과 텍스트 동시 전송 금지 검증
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = content != null && !content.isBlank();

        if (hasFile && hasText) {
            throw new CustomException(ErrorCode.CANNOT_SEND_FILE_AND_TEXT_TOGETHER);
        }

        if (messageType == MessageType.TEXT && !hasText) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (messageType == MessageType.FILE && (file.getSize() == 0)) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
    }

    // S3 fileKey를 받아 DB에 저장 (S3 업로드 책임 없음)
    private Message buildAndSaveMessage(User user, ChatRoom chatRoom, MessageType messageType,
                                        String content, MultipartFile file, String fileKey) {
        if (messageType == MessageType.FILE) {
            Media media = mediaRepository.save(Media.from(user, chatRoom, fileKey, file));
            return messageRepository.save(Message.createWithMedia(chatRoom, user, media, messageType));
        }
        return messageRepository.save(Message.create(chatRoom, user, messageType, content));
    }

    // S3 보상 삭제: DB 저장 실패 시 호출. 삭제 실패는 로그만 남기고 원래 예외를 우선함
    private void deleteUploadedFileQuietly(String fileKey) {
        if (fileKey == null) return;
        try {
            storageService.deleteFile(fileKey);
        } catch (Exception e) {
            log.error("S3 보상 삭제 실패 - 수동 정리 필요. key={}", fileKey, e);
        }
    }

    private boolean isLastMessage(Long deletedMessageId, Optional<Long> lastMessageId) {
        // soft 삭제 이후 1년이 지난 경우를 위해 Optional로 isPresent 사용
        return lastMessageId.isPresent() && lastMessageId.get().equals(deletedMessageId);
    }

    private List<MessageResponse> resolveNewMessages(Long chatRoomId, LocalDateTime since, LocalDateTime enterChatRoomAt) {
        return toResponseList(messageRepository.findNewMessages(chatRoomId, since, enterChatRoomAt));
    }

    private List<MessageResponse> resolveUpdatedMessages(Long chatRoomId, LocalDateTime since, LocalDateTime enterChatRoomAt) {
        if (since == null) return List.of();
        return toResponseList(messageRepository.findUpdatedMessages(chatRoomId, since, enterChatRoomAt));
    }

    private List<Long> resolveDeletedIds(Long chatRoomId, LocalDateTime since, LocalDateTime enterChatRoomAt) {
        if (since == null) return List.of();
        return messageRepository.findDeletedMessageIds(chatRoomId, since, enterChatRoomAt);
    }

    private List<MessageResponse> toResponseList(List<Message> messages) {
        return messages.stream()
                .map(m -> MessageResponse.from(m, resolveFileUrl(m)))
                .collect(Collectors.toList());
    }

    private String resolveFileUrl(Message message) {
        if (message.getMessageType() == MessageType.FILE && message.getMedia() != null) {
            return storageService.getPresignedUrl(message.getMedia().getFileKey());
        }
        return null;
    }
}
