package com.everybuddy.global.util;

import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCleanupScheduler {
    private final MessageRepository messageRepository;

    // soft delete 된지 1년 지난 메시지 새벽 3시마다 자동 삭제
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldDeletedMessages() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        List<Message> messagesToDelete = messageRepository.findMessagesDeletedBefore(oneYearAgo);

        messageRepository.deleteAll(messagesToDelete);
    }
}
