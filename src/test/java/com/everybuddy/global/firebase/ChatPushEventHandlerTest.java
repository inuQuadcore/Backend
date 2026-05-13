package com.everybuddy.global.firebase;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.entity.MessageType;
import com.everybuddy.domain.message.event.MessageSentEvent;
import com.everybuddy.domain.notification.dto.NotificationContent;
import com.everybuddy.domain.notification.service.NotificationMessageBuilder;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatPushEventHandler 단위 테스트")
class ChatPushEventHandlerTest {

    @Mock private ViewingSyncService viewingSyncService;
    @Mock private NotificationMessageBuilder messageBuilder;
    @Mock private FcmSender fcmSender;

    @InjectMocks
    private ChatPushEventHandler handler;

    private User sender;
    private User recipient1;
    private User recipient2;
    private ChatRoom chatRoom;
    private Message message;
    private NotificationContent content;

    @BeforeEach
    void setUp() {
        sender = User.createForTest(1L, "sender", "발신자", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
        recipient1 = User.createForTest(2L, "recipient1", "수신자1", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1991, 1, 1));
        recipient2 = User.createForTest(3L, "recipient2", "수신자2", "password",
                Country.KOREA, Gender.FEMALE, LocalDate.of(1992, 1, 1));
        chatRoom = ChatRoom.createForTest(100L, "테스트방");
        message = Message.createForTest(500L, chatRoom, sender, MessageType.TEXT, "안녕하세요",
                LocalDateTime.of(2026, 5, 5, 10, 0));
        content = NotificationContent.of("발신자", "안녕하세요");
    }

    private MessageSentEvent eventWithParticipants(User... users) {
        List<ChatPart> chatParts = List.of(users).stream()
                .map(user -> ChatPart.create(user, chatRoom))
                .toList();
        return MessageSentEvent.of(message, chatRoom.getChatRoomId(), chatParts, null);
    }

    @Test
    @DisplayName("발신자 본인은 푸시 대상에서 제외된다")
    void excludesSenderFromTargets() {
        when(viewingSyncService.isViewing(any(), any())).thenReturn(false);
        when(messageBuilder.resolveChatMessage(message)).thenReturn(content);
        MessageSentEvent event = eventWithParticipants(sender, recipient1, recipient2);

        handler.handleMessageSent(event);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(fcmSender).sendToUsers(captor.capture(), eq(content), any());
        assertAll(
                () -> assertEquals(2, captor.getValue().size()),
                () -> assertTrue(captor.getValue().contains(2L)),
                () -> assertTrue(captor.getValue().contains(3L))
        );
    }

    @Test
    @DisplayName("채팅방을 보고 있는 유저는 푸시 대상에서 제외된다")
    void excludesViewingUsersFromTargets() {
        when(viewingSyncService.isViewing(2L, 100L)).thenReturn(true);
        when(viewingSyncService.isViewing(3L, 100L)).thenReturn(false);
        when(messageBuilder.resolveChatMessage(message)).thenReturn(content);
        MessageSentEvent event = eventWithParticipants(sender, recipient1, recipient2);

        handler.handleMessageSent(event);

        verify(fcmSender).sendToUsers(eq(List.of(3L)), eq(content), any());
    }

    @Test
    @DisplayName("모든 수신자가 채팅방을 보고 있으면 FcmSender를 호출하지 않는다")
    void doesNotSendWhenAllRecipientsViewing() {
        when(viewingSyncService.isViewing(any(), any())).thenReturn(true);
        MessageSentEvent event = eventWithParticipants(sender, recipient1, recipient2);

        handler.handleMessageSent(event);

        verify(fcmSender, never()).sendToUsers(any(), any(), any());
        verify(messageBuilder, never()).resolveChatMessage(any());
    }

    @Test
    @DisplayName("발신자만 채팅방에 있으면 FcmSender를 호출하지 않는다")
    void doesNotSendWhenOnlySenderInRoom() {
        MessageSentEvent event = eventWithParticipants(sender);

        handler.handleMessageSent(event);

        verify(fcmSender, never()).sendToUsers(any(), any(), any());
        verify(messageBuilder, never()).resolveChatMessage(any());
    }

    @Test
    @DisplayName("정상 발송: data 페이로드에 chatRoomId, messageId, senderId가 포함된다")
    void sendsWithCorrectDataPayload() {
        when(viewingSyncService.isViewing(any(), any())).thenReturn(false);
        when(messageBuilder.resolveChatMessage(message)).thenReturn(content);
        MessageSentEvent event = eventWithParticipants(sender, recipient1);

        handler.handleMessageSent(event);

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmSender).sendToUsers(eq(List.of(2L)), eq(content), dataCaptor.capture());
        Map<String, String> data = dataCaptor.getValue();
        assertAll(
                () -> assertEquals("100", data.get("chatRoomId")),
                () -> assertEquals("500", data.get("messageId")),
                () -> assertEquals("1", data.get("senderId"))
        );
    }
}
