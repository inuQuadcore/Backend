package com.everybuddy.domain.user.repository;

import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.UserLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLanguageRepository extends JpaRepository<UserLanguage, Long> {

    Optional<UserLanguage> findByUserUserIdAndLanguage(Long userId, Language language);
}
