package com.everybuddy.domain.user.repository;

import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.UserLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserLanguageRepository extends JpaRepository<UserLanguage, Long> {

    Optional<UserLanguage> findByUserUserIdAndLanguage(Long userId, Language language);

    Optional<UserLanguage> findByUserUserIdAndIsPrimaryTrue(Long userId);

    @Query("SELECT ul FROM UserLanguage ul WHERE ul.user.userId = :userId")
    List<UserLanguage> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT ul FROM UserLanguage ul WHERE ul.user.userId IN :userIds")
    List<UserLanguage> findAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
