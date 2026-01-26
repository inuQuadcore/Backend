package com.everybuddy.domain.media.repository;

import com.everybuddy.domain.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {

    /**
     * deletedAt이 30일 이상 지난 파일 조회 (hard delete 대상)
     */
    List<Media> findByDeletedAtBefore(LocalDateTime dateTime);
}
