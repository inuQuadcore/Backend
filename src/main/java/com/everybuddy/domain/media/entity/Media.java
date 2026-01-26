package com.everybuddy.domain.media.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "media")
@EntityListeners(AuditingEntityListener.class)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    @Column(nullable = false, unique = true, length = 500)
    private String fileKey;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Boolean deleted;

    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Media(String fileKey, String originalFilename, Long fileSize, String contentType) {
        this.fileKey = fileKey;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public static Media from(String fileKey, String originalFilename, Long fileSize, String contentType) {
        return Media.builder()
                .fileKey(fileKey)
                .originalFilename(originalFilename)
                .fileSize(fileSize)
                .contentType(contentType)
                .build();
    }

    public void softDelete(){
        this.deleted = true;
    }
}
