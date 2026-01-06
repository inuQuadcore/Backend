package com.everybuddy.global.util;

import org.springframework.http.ResponseEntity;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomLogger {
    // ERROR 레벨 로그
    public static ResponseEntity<ErrorResponse> errorLog(String message, ErrorCode errorCode, Exception e) {
        log.error("{}", message, e);
        return ErrorResponse.toResponseEntity(errorCode);
    }

    // WARN 레벨 로그
    public static ResponseEntity<ErrorResponse> warnLog(String message, ErrorCode errorCode) {
        log.warn("{}", message);
        return ErrorResponse.toResponseEntity(errorCode);
    }

}
