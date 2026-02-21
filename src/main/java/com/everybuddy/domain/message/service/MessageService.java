package com.everybuddy.domain.message.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.repository.MediaRepository;
import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.message.dto.ChatRoomMetadata;
import com.everybuddy.domain.message.dto.FirebaseChatMessage;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import org.springframework.dao.DataAccessException;
import com.google.api.core.ApiFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final FirebaseDatabase firebaseDatabase;

    /**
     * 메시지 전송 (파일이 있으면 FILE 메시지, 없으면 TEXT 메시지)
     */
    @Transactional
    public void sendMessage(Long userId, ChatMessageRequest request, MultipartFile file) {
        // 1. 엔티티 조회 및 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        if (chatRoom.isDeleted()) {
            throw new CustomException(ErrorCode.CHATROOM_DELETED);
        }

        if (!chatPartRepository.existsByUserIdAndChatRoomId(userId, request.getChatRoomId())) {
            throw new CustomException(ErrorCode.USER_NOT_IN_CHATROOM);
        }

        // 2. 메시지 타입 자동 판단 및 검증
        boolean hasFile = file != null && !file.isEmpty();
        MessageType messageType = hasFile ? MessageType.FILE : MessageType.TEXT;
        validateMessageType(messageType, request.getContent(), file);

        // 3. S3 업로드: DB 저장보다 먼저 수행
        //    S3 실패 시 여기서 예외 발생 → DB 저장 전이므로 보상 처리 불필요
        String fileKey = hasFile ? storageService.uploadChatFile(chatRoom.getChatRoomId(), file) : null;

        // 4. DB 저장: 실패 시 이미 업로드된 S3 파일 보상 삭제
        try {
            Message message = buildAndSaveMessage(user, chatRoom, messageType, request.getContent(), file, fileKey);

            // Firebase에 필요한 DB 데이터를 트랜잭션 내에서 미리 조회
            Long chatRoomId = chatRoom.getChatRoomId();
            List<ChatPart> chatParts = chatPartRepository.findByChatRoomIdWithUser(chatRoomId);
            ChatRoomMetadata metadata = ChatRoomMetadata.from(message);

            // 커밋 성공 후 Firebase 동기화
            registerAfterCommit(() -> {
                saveMessageToFirebase(message);
                updateChatRoomMetadataInFirebase(chatRoomId, chatParts, buildMetadataUpdates(metadata));
            });
        } catch (DataAccessException e) {
            deleteUploadedFileQuietly(fileKey);
            throw e;
        }
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_MESSAGE_OF_USER);
        }

        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.MESSAGE_ALREADY_DELETED);
        }

        message.softDelete();
        if (message.getMedia() != null) message.getMedia().softDelete();

        // Firebase에 필요한 DB 데이터를 트랜잭션 내에서 미리 조회
        Long chatRoomId = message.getChatRoom().getChatRoomId();
        Optional<Long> lastMessageId = messageRepository.findLastMessageId(message.getChatRoom());
        boolean isLast = isLastMessage(message.getMessageId(), lastMessageId);
        List<ChatPart> chatParts = isLast
                ? chatPartRepository.findByChatRoomIdWithUser(chatRoomId)
                : List.of();

        // 커밋 성공 후 Firebase 동기화
        registerAfterCommit(() -> {
            updateFirebaseAsDeleted(message);
            if (isLast) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("lastMessage", "삭제된 메시지입니다");
                updateChatRoomMetadataInFirebase(chatRoomId, chatParts, updates);
            }
        });
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


    private void saveMessageToFirebase(Message message) {
        String fileUrl = null;
        if (message.getMessageType() == MessageType.FILE && message.getMedia() != null) {
            fileUrl = storageService.getPublicUrl(message.getMedia().getFileKey());
        }
        FirebaseChatMessage messageData = FirebaseChatMessage.from(message, fileUrl);
        DatabaseReference ref = firebaseDatabase.getReference("messages")
                .child(String.valueOf(message.getChatRoom().getChatRoomId()))
                .child(String.valueOf(message.getMessageId()));
        addFirebaseCallback(ref.setValueAsync(messageData), "메시지 저장 messageId=" + message.getMessageId());
    }


    private void updateFirebaseAsDeleted(Message message) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", "삭제된 메시지입니다");
        updates.put("fileUrl", null);
        updates.put("fileName", null);
        updates.put("fileSize", null);
        updates.put("mediaType", null);
        DatabaseReference ref = firebaseDatabase.getReference("messages")
                .child(String.valueOf(message.getChatRoom().getChatRoomId()))
                .child(String.valueOf(message.getMessageId()));
        addFirebaseCallback(ref.updateChildrenAsync(updates), "메시지 삭제 처리 messageId=" + message.getMessageId());
    }


    private boolean isLastMessage(Long deletedMessageId, Optional<Long> lastMessageId) {
        // soft 삭제 이후 1년이 지난 경우를 위해 Optional로 isPresent 사용
        return lastMessageId.isPresent() && lastMessageId.get().equals(deletedMessageId);
    }

    // sendMessage/deleteMessage 공용 - afterCommit 내에서만 호출
    private void updateChatRoomMetadataInFirebase(Long chatRoomId, List<ChatPart> chatParts, Map<String, Object> updates) {
        for (ChatPart chatPart : chatParts) {
            Long uid = chatPart.getUser().getUserId();
            DatabaseReference ref = firebaseDatabase.getReference("users")
                    .child(String.valueOf(uid))
                    .child("chatrooms")
                    .child(String.valueOf(chatRoomId));
            addFirebaseCallback(ref.updateChildrenAsync(updates),
                    "채팅방 메타데이터 업데이트 chatRoomId=" + chatRoomId + " userId=" + uid);
        }
    }

    private Map<String, Object> buildMetadataUpdates(ChatRoomMetadata metadata) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessageId", metadata.getLastMessageId());
        updates.put("lastMessage", metadata.getLastMessage());
        updates.put("lastMessageTime", metadata.getLastMessageTime());
        updates.put("lastMessageSenderId", metadata.getLastMessageSenderId());
        updates.put("lastMessageSenderName", metadata.getLastMessageSenderName());
        return updates;
    }

    private void registerAfterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void addFirebaseCallback(ApiFuture<Void> future, String context) {
        future.addListener(() -> {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Firebase 쓰기 실패 - {}", context, e);
            }
        }, command -> command.run());
    }
}
