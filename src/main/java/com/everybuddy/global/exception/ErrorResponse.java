package com.everybuddy.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.util.Map;


@Getter
public class ErrorResponse {
    private final Integer code;
    private final String name;
    private final String message;
    private final Map<String, String> errors;

    @Builder
    private ErrorResponse(Integer code, String name, String message, Map<String, String> errors) {
        this.code = code;
        this.name = name;
        this.message = message;
        this.errors = errors;
    }

    //비즈니스 로직 예외처리
    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .name(errorCode.name())
                        .message(errorCode.getMessage())
                        .build()
                );
    }

    //유효성 검사 예외처리
    public static ResponseEntity<ErrorResponse> validationFailed(Map<String, String> errors) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .name(errorCode.name())
                        .message(errorCode.getMessage())
                        .errors(errors)
                        .build());
    }
}
