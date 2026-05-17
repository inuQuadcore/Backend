package com.everybuddy.domain.chatroom.repository;

import com.everybuddy.domain.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.isGroup = false " +
            "AND cr.deletedAt IS NULL " +
            "AND EXISTS (SELECT cp1 FROM ChatPart cp1 " +
            "            WHERE cp1.chatRoom = cr " +
            "              AND cp1.user.userId = :userIdA " +
            "              AND cp1.active = true) " +
            "AND EXISTS (SELECT cp2 FROM ChatPart cp2 " +
            "            WHERE cp2.chatRoom = cr " +
            "              AND cp2.user.userId = :userIdB " +
            "              AND cp2.active = true)")
    List<ChatRoom> findActiveDirectChatRooms(@Param("userIdA") Long userIdA,
                                             @Param("userIdB") Long userIdB);
}
