package com.everybuddy.domain.chatpart.entity;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chatpart")
@EntityListeners(AuditingEntityListener.class)
public class ChatPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatPartId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private Message lastReadMessage;

    private LocalDateTime lastReadAt;

    private LocalDateTime exitChatRoomAt;

    private LocalDateTime enterChatRoomAt;

    // isActive로 이름 지으면 Getter 이름 매핑과정에서 문제생김
    private boolean active = true;
}
