package com.everybuddy.global.s3.controller;

import com.everybuddy.global.s3.dto.FileUploadResponse;
import com.everybuddy.global.s3.service.S3FileService;
import com.everybuddy.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final S3FileService s3FileService;

    /**
     * 프로필 이미지 업로드
     */
    @PostMapping("/profile")
    public ResponseEntity<FileUploadResponse> uploadProfileImage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam("file") MultipartFile file) {

        String fileKey = s3FileService.uploadFile(file, "profiles");
        String fileUrl = s3FileService.generatePresignedUrl(fileKey, 60); // 1시간 유효

        FileUploadResponse response = FileUploadResponse.builder()
                .fileKey(fileKey)
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 채팅 이미지/파일 업로드
     */
    @PostMapping("/chat")
    public ResponseEntity<FileUploadResponse> uploadChatFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type) {

        String directory = "chat".equals(type) ? "chat-files" : "chat-images";
        String fileKey = s3FileService.uploadFile(file, directory);
        String fileUrl = s3FileService.generatePresignedUrl(fileKey, 60);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileKey(fileKey)
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 파일 URL 조회 (Presigned URL 재발급)
     */
    @GetMapping("/{fileKey}/url")
    public ResponseEntity<String> getFileUrl(@PathVariable String fileKey) {
        // fileKey는 "directory/filename" 형식으로 전달됨
        String url = s3FileService.generatePresignedUrl(fileKey, 60);
        return ResponseEntity.ok(url);
    }

    /**
     * 파일 삭제
     */
    @DeleteMapping("/{fileKey}")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String fileKey) {

        s3FileService.deleteFile(fileKey);
        return ResponseEntity.noContent().build();
    }
}