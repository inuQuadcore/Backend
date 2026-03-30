package com.everybuddy.domain.user.repository;

import com.everybuddy.domain.user.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {

    @Transactional
    @Modifying
    @Query("UPDATE UserPresence p SET p.isOnline = true, p.lastSeenAt = CURRENT_TIMESTAMP WHERE p.userId = :userId")
    void markOnline(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE UserPresence p SET p.isOnline = false WHERE p.userId = :userId")
    void markOffline(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE UserPresence p SET p.isOnline = false")
    void resetAllOffline();

    @Transactional
    @Modifying
    @Query("UPDATE UserPresence p SET p.isOnline = true, p.lastSeenAt = CURRENT_TIMESTAMP WHERE p.userId IN :userIds")
    void markOnlineBatch(@Param("userIds") List<Long> userIds);
}
