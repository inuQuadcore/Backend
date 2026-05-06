package com.everybuddy.domain.fcmtoken.repository;

import com.everybuddy.domain.fcmtoken.entity.FcmToken;
import com.everybuddy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByUser(User user);

    Optional<FcmToken> findByToken(String token);

    void deleteByUser(User user);

    @Query("SELECT t FROM FcmToken t WHERE t.user.userId IN :userIds")
    List<FcmToken> findAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
