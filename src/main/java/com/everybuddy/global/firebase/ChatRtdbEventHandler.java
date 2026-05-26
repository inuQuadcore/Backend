package com.everybuddy.global.firebase;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatroom.event.ChatRoomCreatedEvent;
import com.everybuddy.domain.chatroom.event.ChatRoomLeftEvent;
import com.everybuddy.domain.chatroom.event.ChatRoomMembersInvitedEvent;
import com.everybuddy.domain.message.dto.ChatRoomMetadata;
import com.everybuddy.domain.message.dto.FirebaseChatMessage;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.message.event.MessageDeletedEvent;
import com.everybuddy.domain.message.event.MessageReadEvent;
import com.everybuddy.domain.message.event.MessageSentEvent;
import com.everybuddy.domain.message.event.MessageUpdatedEvent;
import com.everybuddy.global.s3.service.StorageService;
import com.google.api.core.ApiFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ServerValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.ZoneId;
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
        updateMetadataAndUnreadCountOnSend(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageRead(MessageReadEvent event) {
        String path = "users/" + event.getUserId() + "/chatrooms/" + event.getChatRoomId() + "/unreadCount";
        addFirebaseCallback(
                firebaseDatabase.getReference(path).setValueAsync(0),
                "메시지 읽음 처리 unreadCount=0 userId=" + event.getUserId() + " chatRoomId=" + event.getChatRoomId()
        );
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
        initializeUserChatRoomNodes(event.getChatRoomId(), event.getParticipantIds());
    }

    private void initializeUserChatRoomNodes(Long chatRoomId, List<Long> participantIds) {
        long enterChatRoomAt = System.currentTimeMillis();
        Map<String, Object> multiPathUpdates = new HashMap<>();
        for (Long userId : participantIds) {
            String basePath = "users/" + userId + "/chatrooms/" + chatRoomId;
            multiPathUpdates.put(basePath + "/enterChatRoomAt", enterChatRoomAt);
            multiPathUpdates.put(basePath + "/unreadCount", 0);
        }
        addFirebaseCallback(
                firebaseDatabase.getReference().updateChildrenAsync(multiPathUpdates),
                "채팅방 생성 시 참여자 노드 초기화 chatRoomId=" + chatRoomId
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatRoomMembersInvited(ChatRoomMembersInvitedEvent event) {
        long enterChatRoomAt = System.currentTimeMillis();
        Map<String, Object> multiPathUpdates = new HashMap<>();
        for (Long userId : event.getInvitedUserIds()) {
            String basePath = "users/" + userId + "/chatrooms/" + event.getChatRoomId();
            multiPathUpdates.put(basePath + "/enterChatRoomAt", enterChatRoomAt);
            multiPathUpdates.put(basePath + "/unreadCount", 0);
            multiPathUpdates.put("chatrooms/" + event.getChatRoomId() + "/participants/" + userId, true);
        }
        addFirebaseCallback(
                firebaseDatabase.getReference().updateChildrenAsync(multiPathUpdates),
                "채팅방 멤버 초대 RTDB 노드 초기화 chatRoomId=" + event.getChatRoomId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatRoomLeft(ChatRoomLeftEvent event) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + event.getUserId() + "/chatrooms/" + event.getChatRoomId(), null);
        updates.put("chatrooms/" + event.getChatRoomId() + "/participants/" + event.getUserId(), null);
        addFirebaseCallback(
                firebaseDatabase.getReference().updateChildrenAsync(updates),
                "채팅방 나가기 RTDB 정리 userId=" + event.getUserId() + " chatRoomId=" + event.getChatRoomId()
        );
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
        updates.put("editedAt", message.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
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

    private void updateMetadataAndUnreadCountOnSend(MessageSentEvent event) {
        Long chatRoomId = event.getChatRoomId();
        Long senderId = event.getMessage().getUser().getUserId();
        Map<String, Object> metadataUpdates = buildMetadataUpdates(event.getMetadata());

        Map<String, Object> multiPathUpdates = new HashMap<>();
        for (ChatPart chatPart : event.getChatParts()) {
            Long participantId = chatPart.getUser().getUserId();
            String basePath = "users/" + participantId + "/chatrooms/" + chatRoomId;
            for (Map.Entry<String, Object> entry : metadataUpdates.entrySet()) {
                multiPathUpdates.put(basePath + "/" + entry.getKey(), entry.getValue());
            }
            if (!participantId.equals(senderId)) {
                multiPathUpdates.put(basePath + "/unreadCount", incrementBy(1));
            }
        }

        addFirebaseCallback(
                firebaseDatabase.getReference().updateChildrenAsync(multiPathUpdates),
                "메시지 전송 메타데이터 + unreadCount 업데이트 chatRoomId=" + chatRoomId
        );
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

    private static Object incrementBy(int delta) {
        return ServerValue.increment(delta);
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
