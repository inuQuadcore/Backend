package com.everybuddy.global.s3.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 파일 스토리지 추상화 인터페이스
 * S3, GCS, Local Storage 등 다양한 구현체로 교체 가능
 */
public interface StorageService {

    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @param directory S3 내 디렉토리 (예: "profiles", "chat-files")
     * @return 업로드된 파일의 키 (S3 key)
     */
    String uploadFile(MultipartFile file, String directory);

    /**
     * 단일 파일 삭제
     * @param fileKey 파일 키
     */
    void deleteFile(String fileKey);

    /**
     * 여러 파일 일괄 삭제
     * @param fileKeys 파일 키 리스트
     */
    void deleteFiles(List<String> fileKeys);

    /**
     * 파일의 공개 URL 생성
     * @param fileKey 파일 키
     * @return CloudFront 또는 S3 공개 URL
     */
    String getPublicUrl(String fileKey);

    /**
     * 파일 존재 여부 확인
     * @param fileKey 파일 키
     * @return 존재 여부
     */
    boolean fileExists(String fileKey);
}
