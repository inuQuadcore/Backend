package com.everybuddy.domain.chatpart.repository;

import com.everybuddy.domain.chatpart.entity.ChatPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatPartRepository extends JpaRepository<ChatPart, Long> {

    @Query("SELECT cp FROM ChatPart cp " +
            "JOIN FETCH cp.chatRoom " +
            "WHERE cp.user.userId = :userId AND cp.active = true")
    List<ChatPart> findByUserIdWithChatRoom(@Param("userId") Long userId);

    @Query("SELECT cp FROM ChatPart cp " +
            "JOIN FETCH cp.user " +
            "WHERE cp.chatRoom.chatRoomId IN :chatRoomIds")
    List<ChatPart> findByChatRoomIdsWithUser(@Param("chatRoomIds") List<Long> chatRoomIds);

    @Query("SELECT cp FROM ChatPart cp " +
            "JOIN FETCH cp.user " +
            "WHERE cp.chatRoom.chatRoomId = :chatRoomId AND cp.active = true")
    List<ChatPart> findByChatRoomIdWithUser(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END " +
            "FROM ChatPart cp " +
            "WHERE cp.user.userId = :userId AND cp.chatRoom.chatRoomId = :chatRoomId AND cp.active = true")
    boolean existsByUserIdAndChatRoomId(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);
}
