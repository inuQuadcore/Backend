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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService implements StorageService {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * 프로필 이미지 업로드 (이미지만 허용, 5MB 제한)
     */
    @Override
    public String uploadProfileImage(MultipartFile file, String directory) {
        validateProfileImage(file);
        return upload(file, directory);
    }

    /**
     * 채팅 파일 업로드 (이미지, 비디오 허용, 50MB 제한)
     */
    @Override
    public String uploadChatFile(MultipartFile file, String directory) {
        validateChatFile(file);
        return upload(file, directory);
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
     * 파일의 공개 URL 생성 (S3 직접 접근)
     * @param fileKey 파일 키
     * @return S3 공개 URL
     */
    @Override
    public String getPublicUrl(String fileKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileKey);
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
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일 확장자 검사
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new CustomException(ErrorCode.INVALID_FILE_NAME);
        }

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
     * 채팅 파일 검증 (이미지, 비디오, 50MB)
     */
    private void validateChatFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        // 파일 크기 검사 (50MB)
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일 확장자 검사
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new CustomException(ErrorCode.INVALID_FILE_NAME);
        }

        String extension = extractExtension(originalFilename);
        if (!isChatFileExtension(extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        // MIME 타입 검증 (확장자 조작 방지)
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
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
     * 채팅 파일 확장자 확인 (이미지 + 비디오)
     */
    private boolean isChatFileExtension(String extension) {
        return List.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "mov", "avi", "webm").contains(extension);
    }

    /**
     * 실제 S3 업로드 로직 (HTTP 요청용)
     */
    private String upload(MultipartFile file, String directory) {
        String fileName = generateFileName(file.getOriginalFilename());
        String key = directory + "/" + fileName;

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
            throw new CustomException(ErrorCode.S3_CONNECTION_ERROR);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}