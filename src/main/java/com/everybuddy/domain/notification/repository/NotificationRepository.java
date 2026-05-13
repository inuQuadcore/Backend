package com.everybuddy.domain.notification.repository;

import com.everybuddy.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.toUser.userId = :userId
            ORDER BY n.notificationId DESC
            """)
    List<Notification> findRecentByRecipientUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.toUser.userId = :userId
              AND n.notificationId < :before
            ORDER BY n.notificationId DESC
            """)
    List<Notification> findByRecipientUserIdBeforeCursor(
            @Param("userId") Long userId,
            @Param("before") Long before,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
            FROM Notification n
            WHERE n.toUser.userId = :userId
              AND n.readAt IS NULL
            """)
    boolean existsUnreadByRecipientUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.toUser.userId = :userId
              AND n.readAt IS NULL
            """)
    int markAllReadByRecipientUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
