package com.everybuddy.global.s3.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponse {
    private String fileKey;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String contentType;
}