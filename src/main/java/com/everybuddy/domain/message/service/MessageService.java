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

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatPartRepository chatPartRepository;
    private final FirebaseDatabase firebaseDatabase;

    @Transactional
    public void sendMessage(Long userId, ChatMessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));

        // 채팅방 참여자인지 확인
        if (!chatPartRepository.existsByUserIdAndChatRoomId(userId, request.getChatRoomId()))
            throw new IllegalArgumentException("User is not in ChatRoom");

        Message message = Message.create(chatRoom, user, request);

        messageRepository.save(message);

        saveToFirebase(message);
        updateChatRoomMetadata(message);
    }

    private void saveToFirebase(Message message) {
        DatabaseReference messagesRef = firebaseDatabase.getReference("messages")
                .child(String.valueOf(message.getChatRoom().getChatRoomId()))
                .child(String.valueOf(message.getMessageId()));

        FirebaseChatMessage messageData = FirebaseChatMessage.from(message);

        messagesRef.setValueAsync(messageData);
    }

    private void updateChatRoomMetadata(Message message) {
        Long chatRoomId = message.getChatRoom().getChatRoomId();

        // 채팅방의 모든 참여자 조회
        List<ChatPart> chatParts = chatPartRepository.findByChatRoomIdWithUser(chatRoomId);

        // 각 참여자의 users/{userId}/chatrooms/{chatRoomId} 업데이트
        updateUserChatRoomMetadata(chatRoomId, chatParts, message);
    }

    private void updateUserChatRoomMetadata(Long chatRoomId, List<ChatPart> chatParts, Message message) {
        ChatRoomMetadata metadata = ChatRoomMetadata.from(message);
        Map<String, Object> updates = new HashMap<>();
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
}
