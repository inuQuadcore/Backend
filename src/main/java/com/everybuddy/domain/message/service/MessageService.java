package com.everybuddy.domain.message.service;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatpart.repository.ChatPartRepository;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.chatroom.repository.ChatRoomRepository;
import com.everybuddy.domain.message.dto.ChatMessageRequest;
import com.everybuddy.domain.message.dto.ChatRoomMetadata;
import com.everybuddy.domain.message.dto.FirebaseChatMessage;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final FirebaseDatabase firebaseDatabase;

    @Transactional
    public void sendMessage(Long userId, ChatMessageRequest chatMessageRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(chatMessageRequest.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));

        if (!chatPartRepository.existsByUserIdAndChatRoomId(userId, chatMessageRequest.getChatRoomId()))
            throw new IllegalArgumentException("User is not in ChatRoom");

        Message message = Message.create(chatRoom, user, chatMessageRequest);

        messageRepository.save(message);

        saveToFirebase(message);

        // 채팅방 메타데이터 업데이트
        Long chatRoomId = message.getChatRoom().getChatRoomId();
        List<ChatPart> chatParts = chatPartRepository.findByChatRoomIdWithUser(chatRoomId);
        updateUserChatRoomMetadata(chatRoomId, chatParts, message);
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own messages");
        }

        if (message.isDeleted()) {
            throw new IllegalArgumentException("Message already deleted");
        }

        message.softDelete();

        // RealtimeDB 업데이트
        updateFirebaseAsDeleted(message);
        updateChatRoomMetadataAsDeleted(message);
    }

    @Transactional
    public void markAsRead(Long userId, Long chatRoomId, Long messageId) {
        // 채팅방 참여자 확인 및 조회
        ChatPart chatPart = chatPartRepository.findByUserIdAndChatRoomId(userId, chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("User is not in ChatRoom"));

        Message message = messageRepository
                .findByIdAndChatRoomId(messageId, chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found in this ChatRoom"));

        // 마지막 읽은 메시지 업데이트
        chatPart.updateLastReadMessage(message);
    }


    // 메시지 RealtimeDB에 저장
    private void saveToFirebase(Message message) {
        DatabaseReference messagesRef = firebaseDatabase.getReference("messages")
                .child(String.valueOf(message.getChatRoom().getChatRoomId()))
                .child(String.valueOf(message.getMessageId()));

        FirebaseChatMessage messageData = FirebaseChatMessage.from(message);

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

    // 메시지를 삭제했을 때 RealtimeDB에는 메시지 내용만 "메시지가 삭제되었습니다"로 변경
    private void updateFirebaseAsDeleted(Message message) {
        DatabaseReference messageRef = firebaseDatabase.getReference("messages")
                .child(String.valueOf(message.getChatRoom().getChatRoomId()))
                .child(String.valueOf(message.getMessageId()));

        Map<String, Object> updates = new HashMap<>();
        updates.put("content", "메시지가 삭제되었습니다");

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
            updates.put("lastMessage", "메시지가 삭제되었습니다");

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
