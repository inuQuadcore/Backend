package com.everybuddy.domain.statusmessage.repository;

import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StatusMessageRepository extends JpaRepository<StatusMessage, Long> {

    @Query("SELECT sm FROM StatusMessage sm WHERE sm.user.userId = :userId AND sm.deletedAt IS NULL")
    Optional<StatusMessage> findByUserId(@Param("userId") Long userId);

@Modifying
    @Query("DELETE FROM StatusMessage sm WHERE " +
            "(sm.deletedAt IS NOT NULL AND sm.deletedAt < :threshold) " +
            "OR sm.updatedAt < :threshold")
    int deleteExpiredBeforeOneYear(@Param("threshold") LocalDateTime threshold);
}
