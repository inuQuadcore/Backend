package com.everybuddy.global.s3.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 파일 스토리지 추상화 인터페이스
 * S3, GCS, Local Storage 등 다양한 구현체로 교체 가능
 */
public interface StorageService {

    /**
     * 프로필 이미지 업로드
     * @param userId 사용자 ID
     * @param file 업로드할 파일 (이미지만 허용)
     * @return 업로드된 파일의 키 (S3 key: profiles/user-{userId}/uuid.jpg)
     */
    String uploadProfileImage(Long userId, MultipartFile file);

    /**
     * 채팅 파일 업로드
     * @param chatRoomId 채팅방 ID
     * @param file 업로드할 파일 (이미지, 비디오, 오디오, 문서 허용)
     * @return 업로드된 파일의 키 (S3 key: chat/room-{chatRoomId}/uuid.ext)
     */
    String uploadChatFile(Long chatRoomId, MultipartFile file);

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
     * 파일의 Presigned URL 생성
     * @param fileKey 파일 키
     * @return S3 Presigned URL
     */
    String getPresignedUrl(String fileKey);

    /**
     * 파일 존재 여부 확인
     * @param fileKey 파일 키
     * @return 존재 여부
     */
    boolean fileExists(String fileKey);
}
