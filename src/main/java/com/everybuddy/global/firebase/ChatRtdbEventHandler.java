package com.everybuddy.global.firebase;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatroom.event.ChatRoomCreatedEvent;
import com.everybuddy.domain.message.dto.ChatRoomMetadata;
import com.everybuddy.domain.message.dto.FirebaseChatMessage;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.message.event.MessageDeletedEvent;
import com.everybuddy.domain.message.event.MessageSentEvent;
import com.everybuddy.domain.message.event.MessageUpdatedEvent;
import com.everybuddy.global.s3.service.StorageService;
import com.google.api.core.ApiFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRtdbEventHandler {

    private final FirebaseDatabase firebaseDatabase;
    private final StorageService storageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageSent(MessageSentEvent event) {
        saveMessageToFirebase(event.getMessage());
        updateChatRoomMetadataInFirebase(event.getChatRoomId(), event.getChatParts(), buildMetadataUpdates(event.getMetadata()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageDeleted(MessageDeletedEvent event) {
        updateFirebaseAsDeleted(event.getMessage());
        if (event.isLast()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", "삭제된 메시지입니다");
            updateChatRoomMetadataInFirebase(event.getChatRoomId(), event.getChatParts(), updates);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageUpdated(MessageUpdatedEvent event) {
        updateFirebaseAsEdited(event.getMessage());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatRoomCreated(ChatRoomCreatedEvent event) {
        saveParticipantsToFirebase(event.getChatRoomId(), event.getParticipantIds());
    }

    private void saveMessageToFirebase(Message message) {
        String fileUrl = null;
        if (message.getMessageType() == MessageType.FILE && message.getMedia() != null) {
            fileUrl = storageService.getPresignedUrl(message.getMedia().getFileKey());
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

    private void updateFirebaseAsEdited(Message message) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", message.getContent());
        DatabaseReference ref = firebaseDatabase.getReference("messages")
                .child(String.valueOf(message.getChatRoom().getChatRoomId()))
                .child(String.valueOf(message.getMessageId()));
        addFirebaseCallback(ref.updateChildrenAsync(updates), "메시지 수정 messageId=" + message.getMessageId());
    }

    private void saveParticipantsToFirebase(Long chatRoomId, List<Long> participantIds) {
        Map<String, Boolean> participantsMap = new HashMap<>();
        for (Long participantId : participantIds) {
            participantsMap.put(String.valueOf(participantId), true);
        }
        DatabaseReference ref = firebaseDatabase.getReference("chatrooms")
                .child(String.valueOf(chatRoomId))
                .child("participants");
        addFirebaseCallback(ref.setValueAsync(participantsMap), "채팅방 참여자 저장 chatRoomId=" + chatRoomId);
    }

    private void updateChatRoomMetadataInFirebase(Long chatRoomId, List<ChatPart> chatParts, Map<String, Object> updates) {
        Map<String, Object> multiPathUpdates = new HashMap<>();
        for (ChatPart chatPart : chatParts) {
            String basePath = "users/" + chatPart.getUser().getUserId() + "/chatrooms/" + chatRoomId;
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                multiPathUpdates.put(basePath + "/" + entry.getKey(), entry.getValue());
            }
        }
        addFirebaseCallback(
                firebaseDatabase.getReference().updateChildrenAsync(multiPathUpdates),
                "채팅방 메타데이터 업데이트 chatRoomId=" + chatRoomId
        );
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

    private void addFirebaseCallback(ApiFuture<Void> future, String context) {
        future.addListener(() -> {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Firebase 쓰기 인터럽트 - {}", context, e);
            } catch (Exception e) {
                log.error("Firebase 쓰기 실패 - {}", context, e);
            }
        }, Runnable::run);
    }
}
