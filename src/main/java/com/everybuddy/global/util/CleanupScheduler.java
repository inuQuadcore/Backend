package com.everybuddy.global.util;

import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.repository.MediaRepository;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.s3.service.S3FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final MessageRepository messageRepository;
    private final MediaRepository mediaRepository;
    private final S3FileService s3FileService;

    /**
     * 매일 새벽 3시에 삭제된 데이터 정리
     * - 1년 지난 데이터들
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupDeletedData() {
        cleanupOldMessages();
        cleanupOldMedia();
    }

    private void cleanupOldMessages() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        try {
            List<Message> messagesToDelete = messageRepository.findMessagesDeletedBefore(oneYearAgo);

            if (messagesToDelete.isEmpty()){
                return;
            }

            messageRepository.deleteAll(messagesToDelete);

            log.info("삭제 완료: 메시지 개수 = {}", messagesToDelete.size());

        } catch (DataAccessException e) {
            log.error("메시지 삭제 실패, ", e);
        }
    }

    private void cleanupOldMedia() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        List<Media> mediaToDelete = mediaRepository.findByDeletedAtBefore(oneYearAgo);

        if (mediaToDelete.isEmpty()) {
            return;
        }

        List<String> fileKeys = mediaToDelete.stream()
                .map(Media::getFileKey)
                .toList();

        List<Long> mediaIds = mediaToDelete.stream()
                .map(Media::getMediaId)
                .toList();

        try {
            s3FileService.deleteFiles(fileKeys);
            mediaRepository.deleteAllByIdInBatch(mediaIds);

            log.info("파일 삭제 완료: 개수 = {}", mediaToDelete.size());

        } catch (S3Exception e) {
            log.error("S3 파일 일괄 삭제 실패: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            log.error("DB에서 미디어 일괄 삭제 실패", e);
        }
    }
}
