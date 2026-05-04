package com.everybuddy.domain.fcmtoken.repository;

import com.everybuddy.domain.fcmtoken.entity.FcmToken;
import com.everybuddy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByUser(User user);

    void deleteByUser(User user);
}
