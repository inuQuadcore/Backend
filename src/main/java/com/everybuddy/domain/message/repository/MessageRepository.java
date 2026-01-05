package com.everybuddy.domain.message.repository;

import com.everybuddy.domain.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.chatRoom.chatRoomId = :chatRoomId " +
            "AND (:lastReadMessageId IS NULL OR m.messageId > :lastReadMessageId)")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                             @Param("lastReadMessageId") Long lastReadMessageId);
}
