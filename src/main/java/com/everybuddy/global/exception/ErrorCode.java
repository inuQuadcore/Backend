package com.everybuddy.global.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 유저를 찾을 수 없습니다."),
    USER_DELETED(HttpStatus.GONE, 410, "삭제된 사용자입니다."),
    DUPLICATED_USER(HttpStatus.CONFLICT, 409, "이미 존재하는 유저입니다."),
    CANNOT_ADD_SELF(HttpStatus.BAD_REQUEST, 400, "자기 자신을 친구로 추가할 수 없습니다."),
    CANNOT_DELETE_SELF(HttpStatus.BAD_REQUEST, 400, "자기 자신을 친구에서 삭제할 수 없습니다."),
    ALREADY_FRIEND(HttpStatus.CONFLICT, 409, "이미 친구인 사용자입니다."),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "친구 관계를 찾을 수 없습니다."),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, 400, "자기 자신을 차단할 수 없습니다."),
    ALREADY_BLOCKED(HttpStatus.CONFLICT, 409, "이미 차단한 사용자입니다."),
    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "차단 관계를 찾을 수 없습니다."),
    STATUS_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "상태메시지를 찾을 수 없습니다."),
    STATUS_MESSAGE_ALREADY_EXISTS(HttpStatus.CONFLICT, 409, "이미 상태메시지가 존재합니다."),
    STATUS_MESSAGE_EXPIRED(HttpStatus.BAD_REQUEST, 400, "만료된 상태메시지입니다."),
    USER_LANGUAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 언어가 관심 언어 목록에 없습니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "요청한 참여자를 찾을 수 없습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 400, "잘못된 입력입니다."),
    CANNOT_SEND_FILE_AND_TEXT_TOGETHER(HttpStatus.BAD_REQUEST, 400, "파일과 텍스트를 동시에 전송할 수 없습니다."),
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 채팅방을 찾을 수 없습니다."),
    CHATROOM_DELETED(HttpStatus.GONE, 410, "삭제된 채팅방입니다."),
    USER_NOT_IN_CHATROOM(HttpStatus.FORBIDDEN, 403, "해당 채팅방에 접근할 권한이 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 메시지를 찾을 수 없습니다."),
    MESSAGE_ALREADY_DELETED(HttpStatus.CONFLICT, 409, "이미 삭제된 메시지입니다."),
    NOT_MESSAGE_OF_USER(HttpStatus.FORBIDDEN, 403, "자신의 메시지만 수정/삭제할 수 있습니다."),
    MESSAGE_EDIT_TIME_EXCEEDED(HttpStatus.FORBIDDEN, 403, "메시지 수정/삭제 가능 시간이 지났습니다. (최대 5분)"),
    CANNOT_EDIT_FILE_MESSAGE(HttpStatus.BAD_REQUEST, 400, "파일 메시지는 수정할 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "알림을 찾을 수 없습니다."),
    NOT_NOTIFICATION_OF_USER(HttpStatus.FORBIDDEN, 403, "자신의 알림만 읽음 처리할 수 있습니다."),

    //파일 업로드 관련
    MULTIPART_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "파일 읽기에 실패했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, 400, "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, 413, "파일 크기가 제한을 초과했습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, 400, "빈 파일은 업로드할 수 없습니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, 400, "유효하지 않은 파일명입니다."),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, 400, "업로드 가능한 파일 개수를 초과했습니다."),
    S3_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "파일 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    //JWT
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, 401, "유효하지 않은 리프레쉬 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 401, "리프레쉬 토큰이 만료되었습니다. 다시 로그인해주세요."),
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
    INVALID_OAUTH_TOKEN(HttpStatus.UNAUTHORIZED, 401, "유효하지 않은 OAuth 토큰입니다."),
    TEMP_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 401, "임시 토큰이 만료되었습니다. 다시 구글 로그인을 시도해주세요."),

    //데이터베이스 관련
    DATABASE_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, 409, "이미 존재하는 데이터이거나 처리할 수 없는 요청입니다."),

    // firebase 관련
    FIREBASE_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    FIREBASE_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // 번역 관련
    UNSUPPORTED_LANGUAGE(HttpStatus.BAD_REQUEST, 400, "지원하지 않는 언어 코드입니다."),
    USER_PRIMARY_LANGUAGE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, 500, "사용자의 주 언어 정보를 찾을 수 없습니다."),
    INVALID_AUDIO_FORMAT(HttpStatus.BAD_REQUEST, 400, "지원하지 않는 오디오 형식입니다."),
    AUDIO_FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, 413, "오디오 파일 크기가 제한을 초과했습니다. (최대 50MB)"),
    MODEL_ERROR(HttpStatus.BAD_GATEWAY, 502, "번역 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    MODEL_REQUEST_INVALID(HttpStatus.BAD_GATEWAY, 502, "번역 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    MODEL_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, 504, "번역 요청 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."),
    MODEL_UNAVAILABLE(HttpStatus.BAD_GATEWAY, 502, "번역 서비스를 현재 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
