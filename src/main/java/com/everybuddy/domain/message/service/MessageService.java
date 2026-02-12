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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        if (!chatPartRepository.existsByUserIdAndChatRoomId(userId, request.getChatRoomId())) {
            throw new CustomException(ErrorCode.USER_NOT_IN_CHATROOM);
        }

        // 2. 메시지 타입 자동 판단 및 검증
        MessageType messageType = (file != null) ? MessageType.FILE : MessageType.TEXT;
        validateMessageType(messageType, request.getContent(), file);

        // 3. 메시지 생성 및 저장
        Message message = createMessage(user, chatRoom, messageType, request.getContent(), file);
        messageRepository.save(message);

        // 4. 실시간 전파
        publishMessageToFirebase(message);
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

        // 첨부 파일이 있다면 Media도 soft delete
        if (message.getMedia() != null) {
            message.getMedia().softDelete();
        }

        // RealtimeDB 업데이트
        updateFirebaseAsDeleted(message);
        updateChatRoomMetadataAsDeleted(message);
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
        if (messageType == MessageType.TEXT && (content == null || content.isBlank())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (messageType == MessageType.FILE && file == null) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
    }

    /**
     * 메시지 타입에 따라 적절한 메시지 엔티티 생성
     */
    private Message createMessage(
            User user,
            ChatRoom chatRoom,
            MessageType messageType,
            String content,
            MultipartFile file) {

        if (messageType == MessageType.FILE) {
            Media media = createMediaFromFile(user, chatRoom, file);
            return Message.createWithMedia(chatRoom, user, media, messageType);
        }
        return Message.create(chatRoom, user, messageType, content);
    }

    /**
     * 파일 업로드 후 Media 엔티티 생성
     */
    private Media createMediaFromFile(User user, ChatRoom chatRoom, MultipartFile file) {
        String fileKey = storageService.uploadChatFile(chatRoom.getChatRoomId(), file);
        Media media = Media.from(user, chatRoom, fileKey, file);
        return mediaRepository.save(media);
    }

    /**
     * Firebase 및 채팅방 메타데이터 업데이트
     */
    private void publishMessageToFirebase(Message message) {
        saveMessageToFirebase(message);

        Long chatRoomId = message.getChatRoom().getChatRoomId();
        List<ChatPart> chatParts = chatPartRepository.findByChatRoomIdWithUser(chatRoomId);
        updateUserChatRoomMetadata(chatRoomId, chatParts, message);
    }

    // 메시지 RealtimeDB에 저장
    private void saveMessageToFirebase(Message message) {
        DatabaseReference messagesRef = firebaseDatabase.getReference("messages")
                .child(String.valueOf(message.getChatRoom().getChatRoomId()))
                .child(String.valueOf(message.getMessageId()));

        // 파일 메시지인 경우 S3 공개 URL 생성
        String fileUrl = null;
        if (message.getMessageType() == MessageType.FILE && message.getMedia() != null) {
            fileUrl = storageService.getPublicUrl(message.getMedia().getFileKey());
        }

        FirebaseChatMessage messageData = FirebaseChatMessage.from(message, fileUrl);

        messagesRef.setValueAsync(messageData);
    }

    // 채팅방 리스트에서 실시간 업데이트를 하기 위해, RealtimeDB로 메시지를 전송할 때 채팅방에 있는 유저들의 채팅방 메타데이터 업데이트
    private void updateUserChatRoomMetadata(Long chatRoomId, List<ChatPart> chatParts, Message message) {
        ChatRoomMetadata metadata = ChatRoomMetadata.from(message);
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessageId", metadata.getLastMessageId());
        updates.put("lastMessage", metadata.getLastMessage());
        updates.put("lastMessageTime", metadata.getLastMessageTime());
        updates.put("lastMessageSenderId", metadata.getLastMessageSenderId());
        updates.put("lastMessageSenderName", metadata.getLastMessageSenderName());

        for (ChatPart chatPart : chatParts) {
            Long userId = chatPart.getUser().getUserId();
            DatabaseReference userChatRoomRef = firebaseDatabase.getReference("users")
                    .child(String.valueOf(userId))
                    .child("chatrooms")
                    .child(String.valueOf(chatRoomId));

            userChatRoomRef.updateChildrenAsync(updates);
        }
    }

    // 메시지를 삭제했을 때 RealtimeDB 업데이트
    private void updateFirebaseAsDeleted(Message message) {
        DatabaseReference messageRef = firebaseDatabase.getReference("messages")
                .child(String.valueOf(message.getChatRoom().getChatRoomId()))
                .child(String.valueOf(message.getMessageId()));

        Map<String, Object> updates = new HashMap<>();
        updates.put("content", "삭제된 메시지입니다");

        // 파일 관련 필드 null 처리
        updates.put("fileUrl", null);
        updates.put("fileName", null);
        updates.put("fileSize", null);
        updates.put("mediaType", null);

        messageRef.updateChildrenAsync(updates);
    }


    private void updateChatRoomMetadataAsDeleted(Message deletedMessage) {
        ChatRoom chatRoom = deletedMessage.getChatRoom();
        Long chatRoomId = chatRoom.getChatRoomId();

        Optional<Long> lastMessageId = messageRepository.findLastMessageId(chatRoom);

        // 삭제하려는 메시지가 마지막 메시지인 경우에만 메타데이터 업데이트
        if (isLastMessage(deletedMessage.getMessageId(), lastMessageId)) {
            List<ChatPart> chatParts = chatPartRepository.findByChatRoomIdWithUser(chatRoomId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", "삭제된 메시지입니다");

            updateUserChatRoomMetadataFields(chatRoomId, chatParts, updates);
        }
    }

    private boolean isLastMessage(Long deletedMessageId, Optional<Long> lastMessageId) {
        // soft 삭제 이후 1년이 지난 경우를 위해 Optional로 isPresent 사용
        return lastMessageId.isPresent() && lastMessageId.get().equals(deletedMessageId);
    }

    private void updateUserChatRoomMetadataFields(Long chatRoomId, List<ChatPart> chatParts, Map<String, Object> updates) {
        for (ChatPart chatPart : chatParts) {
            Long userId = chatPart.getUser().getUserId();
            DatabaseReference userChatRoomRef = firebaseDatabase.getReference("users")
                    .child(String.valueOf(userId))
                    .child("chatrooms")
                    .child(String.valueOf(chatRoomId));

            userChatRoomRef.updateChildrenAsync(updates);
        }
    }
}
