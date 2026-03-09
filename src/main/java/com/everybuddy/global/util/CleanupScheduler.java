package com.everybuddy.global.util;

import com.everybuddy.domain.media.entity.Media;
import com.everybuddy.domain.media.repository.MediaRepository;
import com.everybuddy.domain.message.entity.Message;
import com.everybuddy.domain.message.repository.MessageRepository;
import com.everybuddy.domain.statusmessage.repository.StatusMessageRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.s3.service.S3FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final MessageRepository messageRepository;
    private final MediaRepository mediaRepository;
    private final StatusMessageRepository statusMessageRepository;
    private final S3FileService s3FileService;

    /**
     * 매일 새벽 3시에 삭제된 데이터 정리
     * - soft delete 후 1년이 지난 데이터 물리 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupDeletedData() {
        cleanupOldMessages();
        cleanupOldMedia();
        cleanupOldStatusMessages();
    }

    private void cleanupOldMessages() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        List<Message> messagesToDelete = messageRepository.findMessagesDeletedBefore(oneYearAgo);

        if (messagesToDelete.isEmpty()) {
            return;
        }

        try {
            messageRepository.deleteAll(messagesToDelete);
            log.info("메시지 물리 삭제 완료: {}건", messagesToDelete.size());
        } catch (DataAccessException e) {
            log.error("메시지 물리 삭제 실패", e);
            throw e;
        }
    }

    private void cleanupOldStatusMessages() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        try {
            int deleted = statusMessageRepository.deleteExpiredBeforeOneYear(oneYearAgo);
            log.info("상태메시지 물리 삭제 완료: {}건", deleted);
        } catch (DataAccessException e) {
            log.error("상태메시지 물리 삭제 실패", e);
            throw e;
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

        // DB 먼저 삭제: 실패 시 트랜잭션 롤백으로 일관성 유지
        try {
            mediaRepository.deleteAllByIdInBatch(mediaIds);
            log.info("미디어 DB 물리 삭제 완료: {}건", mediaIds.size());
        } catch (DataAccessException e) {
            log.error("미디어 DB 물리 삭제 실패", e);
            throw e;
        }

        // S3 삭제: 트랜잭션 대상 아님. 실패해도 DB는 이미 커밋되므로 로그 후 종료.
        // S3에 고아 파일이 남을 수 있으나 DB 참조가 없으므로 서비스 영향 없음.
        try {
            s3FileService.deleteFiles(fileKeys);
            log.info("S3 파일 삭제 완료: {}건", fileKeys.size());
        } catch (CustomException e) {
            log.error("S3 파일 삭제 실패 - 수동 정리 필요. keys={}", fileKeys, e);
        }
    }
}
