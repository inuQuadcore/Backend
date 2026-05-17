package com.everybuddy.global.s3.service;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * 프로필 이미지 업로드 (이미지만 허용, 5MB 제한)
     */
    @Override
    public String uploadProfileImage(Long userId, MultipartFile file) {
        validateProfileImage(file);
        String directory = "profiles/user-" + userId;
        String fileName = generateFileName(file.getOriginalFilename());
        String key = directory + "/" + fileName;
        return upload(file, key);
    }

    /**
     * 채팅 파일 업로드 (이미지, 비디오, 오디오, 문서 허용, 10MB 제한)
     */
    @Override
    public String uploadChatFile(Long chatRoomId, MultipartFile file) {
        validateChatFile(file);
        String directory = "chat/room-" + chatRoomId;
        String fileName = generateFileName(file.getOriginalFilename());
        String key = directory + "/" + fileName;
        return upload(file, key);
    }

    /**
     * 파일 삭제
     * @param key S3 객체 키
     */
    @Override
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (S3Exception | SdkClientException e) {
            throw new CustomException(ErrorCode.S3_CONNECTION_ERROR);
        }
    }

    /**
     * 여러 파일 일괄 삭제
     * @param keys S3 객체 키 리스트
     */
    @Override
    public void deleteFiles(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        try {
            List<ObjectIdentifier> objectIdentifiers = keys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();

            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectIdentifiers).build())
                    .build();

            s3Client.deleteObjects(deleteObjectsRequest);

        } catch (S3Exception | SdkClientException e){
            throw new CustomException(ErrorCode.S3_CONNECTION_ERROR);
        }
    }

    /**
     * Presigned URL 생성 (버킷 비공개 유지, 7일 유효)
     * @param fileKey 파일 키
     * @return 서명된 임시 URL
     */
    @Override
    public String getPublicUrl(String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(7))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * 파일 존재 여부 확인
     * @param key S3 객체 키
     * @return 존재 여부
     */
    @Override
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * 고유한 파일명 생성
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    /**
     * 프로필 이미지 검증 (이미지만, 5MB)
     */
    private void validateProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        // 파일 크기 검사 (5MB)
        long maxSize = 5L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일 이름 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new CustomException(ErrorCode.INVALID_FILE_NAME);
        }

        // 파일 확장자 검증
        String extension = extractExtension(originalFilename);
        if (!isImageExtension(extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        // MIME 타입 검증 (확장자 조작 방지)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    /**
     * 채팅 파일 검증 (이미지, 비디오, 오디오, 문서, 압축, 10MB)
     */
    private void validateChatFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        // 파일 크기 검사 (10MB)
        long maxSize = 10L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일 이름 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new CustomException(ErrorCode.INVALID_FILE_NAME);
        }

        // 파일 확장자 검증
        String extension = extractExtension(originalFilename);
        if (!isChatFileExtension(extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        // MIME 타입 검증 (확장자 조작 방지)
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    /**
     * 허용된 MIME 타입 확인
     */
    private boolean isAllowedContentType(String contentType) {
        return contentType.startsWith("image/")
                || contentType.startsWith("video/")
                || contentType.startsWith("audio/")
                || isDocumentContentType(contentType);
    }

    /**
     * 문서 MIME 타입 확인
     */
    private boolean isDocumentContentType(String contentType) {
        return contentType.equals("application/pdf")
                || contentType.equals("text/plain")
                || contentType.equals("application/msword")  // .doc
                || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")  // .docx
                || contentType.equals("application/vnd.ms-excel")  // .xls
                || contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")  // .xlsx
                || contentType.equals("application/vnd.ms-powerpoint")  // .ppt
                || contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")  // .pptx
                || contentType.equals("application/zip")
                || contentType.equals("application/x-rar-compressed")
                || contentType.equals("application/x-zip-compressed");
    }

    /**
     * 파일 확장자 추출
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".") || filename.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_FILE_NAME);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 이미지 확장자 확인
     */
    private boolean isImageExtension(String extension) {
        return List.of("jpg", "jpeg", "png", "gif", "webp", "heic").contains(extension);
    }

    /**
     * 비디오 확장자 확인
     */
    private boolean isVideoExtension(String extension) {
        return List.of("mp4", "mov", "avi", "webm").contains(extension);
    }

    /**
     * 오디오 확장자 확인
     */
    private boolean isAudioExtension(String extension) {
        return List.of("mp3", "wav", "m4a", "aac").contains(extension);
    }

    /**
     * 문서 확장자 확인
     */
    private boolean isDocumentExtension(String extension) {
        return List.of("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx").contains(extension);
    }

    /**
     * 압축 파일 확장자 확인
     */
    private boolean isArchiveExtension(String extension) {
        return List.of("zip", "rar").contains(extension);
    }

    /**
     * 채팅 파일 확장자 확인 (이미지 + 비디오 + 오디오 + 문서 + 압축)
     */
    private boolean isChatFileExtension(String extension) {
        return isImageExtension(extension)
                || isVideoExtension(extension)
                || isAudioExtension(extension)
                || isDocumentExtension(extension)
                || isArchiveExtension(extension);
    }

    /**
     * 실제 S3 업로드 로직
     */
    private String upload(MultipartFile file, String key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return key;

        } catch (S3Exception | SdkClientException e) {
            log.error("S3 업로드 실패 - Bucket: {}, Key: {}, 원인: {}", bucketName, key, e.getMessage(), e);
            throw new CustomException(ErrorCode.S3_CONNECTION_ERROR);
        } catch (IOException e) {
            log.error("파일 읽기 실패 - Key: {}, 원인: {}", key, e.getMessage(), e);
            throw new CustomException(ErrorCode.MULTIPART_READ_FAILED);
        }
    }
}