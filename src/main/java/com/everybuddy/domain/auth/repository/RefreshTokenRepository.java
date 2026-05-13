package com.everybuddy.domain.auth.repository;

import com.everybuddy.domain.auth.entity.RefreshToken;
import com.everybuddy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    void deleteByToken(String token);

    void deleteByUser(User user);
}
