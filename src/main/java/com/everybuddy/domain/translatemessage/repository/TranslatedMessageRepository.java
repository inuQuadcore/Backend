package com.everybuddy.domain.translatemessage.repository;

import com.everybuddy.domain.translatemessage.entity.TranslatedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslatedMessageRepository extends JpaRepository<TranslatedMessage, Long> {
}
