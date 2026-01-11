package com.everybuddy.domain.message.repository;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import com.everybuddy.domain.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.chatRoom.chatRoomId = :chatRoomId " +
            "AND (:lastReadMessageId IS NULL OR m.messageId > :lastReadMessageId) " +
            "AND m.deletedAt IS NULL")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                             @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("SELECT m.messageId FROM Message m " +
            "WHERE m.chatRoom = :chatRoom " +
            "ORDER BY m.messageId DESC " +
            "LIMIT 1")
    Optional<Long> findLastMessageId(@Param("chatRoom") ChatRoom chatRoom);

    @Query("SELECT m FROM Message m " +
            "WHERE m.deletedAt IS NOT NULL " +
            "AND m.deletedAt < :threshold")
    List<Message> findMessagesDeletedBefore(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT m FROM Message m " +
            "WHERE m.messageId = :messageId " +
            "AND m.chatRoom.chatRoomId = :chatRoomId")
    Optional<Message> findByIdAndChatRoomId(@Param("messageId") Long messageId,
                                            @Param("chatRoomId") Long chatRoomId);
}
