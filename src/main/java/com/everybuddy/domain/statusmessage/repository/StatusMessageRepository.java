package com.everybuddy.domain.statusmessage.repository;

import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StatusMessageRepository extends JpaRepository<StatusMessage, Long> {

    @Query("SELECT sm FROM StatusMessage sm WHERE sm.user.userId = :userId AND sm.deletedAt IS NULL")
    Optional<StatusMessage> findByUserId(@Param("userId") Long userId);

    @Query("SELECT sm FROM StatusMessage sm JOIN FETCH sm.user WHERE sm.user.userId = :userId AND sm.deletedAt IS NULL")
    Optional<StatusMessage> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("""
            SELECT sm FROM StatusMessage sm JOIN FETCH sm.user u
            WHERE sm.deletedAt IS NULL AND u.deletedAt IS NULL
              AND EXISTS (
                  SELECT 1 FROM FriendRelation fr
                  WHERE (fr.fromUser.userId = :myUserId AND fr.toUser.userId = u.userId)
                     OR (fr.toUser.userId = :myUserId AND fr.fromUser.userId = u.userId)
              )
            ORDER BY sm.updatedAt DESC, sm.statusMessageId DESC
            """)
    List<StatusMessage> findFriendStatusMessages(@Param("myUserId") Long myUserId, Pageable pageable);

    @Query("""
            SELECT sm FROM StatusMessage sm JOIN FETCH sm.user u
            WHERE sm.deletedAt IS NULL AND u.deletedAt IS NULL
              AND EXISTS (
                  SELECT 1 FROM FriendRelation fr
                  WHERE (fr.fromUser.userId = :myUserId AND fr.toUser.userId = u.userId)
                     OR (fr.toUser.userId = :myUserId AND fr.fromUser.userId = u.userId)
              )
              AND (sm.updatedAt < :cursorUpdatedAt
                   OR (sm.updatedAt = :cursorUpdatedAt AND sm.statusMessageId < :cursorId))
            ORDER BY sm.updatedAt DESC, sm.statusMessageId DESC
            """)
    List<StatusMessage> findFriendStatusMessagesAfterCursor(
            @Param("myUserId") Long myUserId,
            @Param("cursorUpdatedAt") LocalDateTime cursorUpdatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Modifying
    @Query("DELETE FROM StatusMessage sm WHERE " +
            "(sm.deletedAt IS NOT NULL AND sm.deletedAt < :threshold) " +
            "OR sm.updatedAt < :threshold")
    int deleteExpiredBeforeOneYear(@Param("threshold") LocalDateTime threshold);
}
