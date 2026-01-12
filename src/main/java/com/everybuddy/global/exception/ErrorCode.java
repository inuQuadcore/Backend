package com.everybuddy.global.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 유저를 찾을 수 없습니다."),
    DUPLICATED_USER(HttpStatus.CONFLICT, 409, "이미 존재하는 유저입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 400, "잘못된 입력입니다."),
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 채팅방을 찾을 수 없습니다."),
    USER_NOT_IN_CHATROOM(HttpStatus.FORBIDDEN, 403, "해당 채팅방에 접근할 권한이 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 메시지를 찾을 수 없습니다."),
    MESSAGE_ALREADY_DELETED(HttpStatus.CONFLICT, 409, "이미 삭제된 메시지입니다."),
    NOT_MESSAGE_OF_USER(HttpStatus.FORBIDDEN, 403, "자신의 메시지만 삭제할 수 있습니다."),

    //파일 업로드 관련
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "파일을 업로드할 수 없습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, 400, "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, 413, "파일 크기가 제한을 초과했습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, 400, "빈 파일은 업로드할 수 없습니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, 400, "유효하지 않은 파일명입니다."),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, 400, "업로드 가능한 파일 개수를 초과했습니다."),
    S3_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "파일 업로드 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    //JWT
    JWT_ENTRY_POINT(HttpStatus.UNAUTHORIZED, 401, "로그인이 필요합니다."),
    JWT_ACCESS_DENIED(HttpStatus.FORBIDDEN, 403, "접근 권한이 없습니다."),
    JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, 401, "인증에 실패했습니다. 다시 로그인해주세요."),
    JWT_MALFORMED(HttpStatus.UNAUTHORIZED, 401, "인증에 실패했습니다. 다시 로그인해주세요."),
    JWT_ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 401, "로그인이 만료되었습니다. 다시 로그인해주세요."),
    JWT_UNSUPPORTED(HttpStatus.UNAUTHORIZED, 401, "인증에 실패했습니다. 다시 로그인해주세요."),
    JWT_NOT_VALID(HttpStatus.UNAUTHORIZED, 401, "인증에 실패했습니다. 다시 로그인해주세요."),

    //HTTP 요청 관련
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 405, "잘못된 요청 방식입니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, 400, "요청에 필요한 입력이 누락되었습니다."),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, 400, "입력 형식이 올바르지 않습니다."),
    API_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "요청한 페이지를 찾을 수 없습니다."),

    //인증 관련
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, 401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.UNAUTHORIZED, 401, "비밀번호가 일치하지 않습니다."),

    //데이터베이스 관련
    DATABASE_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, 409, "이미 존재하는 데이터이거나 처리할 수 없는 요청입니다."),

    // firebase 관련
    FIREBASE_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    FIREBASE_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
