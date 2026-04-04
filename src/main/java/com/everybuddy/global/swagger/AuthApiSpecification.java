package com.everybuddy.global.swagger;

import com.everybuddy.domain.auth.dto.FirebaseTokenResponse;
import com.everybuddy.domain.auth.dto.GoogleLoginRequest;
import com.everybuddy.domain.auth.dto.GoogleLoginResponse;
import com.everybuddy.domain.auth.dto.GoogleRegisterRequest;
import com.everybuddy.domain.auth.dto.LoginRequest;
import com.everybuddy.domain.auth.dto.LoginResponse;
import com.everybuddy.domain.auth.dto.RegisterRequest;
import com.everybuddy.domain.auth.dto.TokenRefreshRequest;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import com.google.firebase.auth.FirebaseAuthException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "인증 API", description = "회원가입·로그인·Firebase 토큰 발급")
public interface AuthApiSpecification {

    @SecurityRequirements(value = {})
    @Operation(summary = "회원가입", description = "신규 회원 가입")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "개인정보 약관 미동의", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "개인정보 동의가 필요합니다."
                                    }
                                    """),
                                    @ExampleObject(name = "관심 언어 미입력", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "관심 언어를 하나 이상 선택해주세요."
                                    }
                                    """),
                                    @ExampleObject(name = "관심 태그 미입력", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "관심 태그를 하나 이상 선택해주세요."
                                    }
                                    """),
                                    @ExampleObject(name = "잘못된 국적/성별/언어/태그 값", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다."
                                    }
                                    """),
                                    @ExampleObject(name = "잘못된 생년월일 형식", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다."
                                    }
                                    """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409", description = "계정 중복",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 409,
                        "name": "DUPLICATED_USER",
                        "message": "이미 존재하는 유저입니다."
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<Void> createUser(@Valid @RequestBody RegisterRequest registerRequest);

    @SecurityRequirements(value = {})
    @Operation(summary = "로그인", description = "로그인 및 JWT 토큰 발급")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject("""
                    {
                        "userId": 123,
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "tokenType": "Bearer",
                        "accessTokenExpiresAt": "2026-04-30T13:00:00",
                        "refreshTokenExpiresAt": "2026-05-30T12:00:00"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401", description = "로그인 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 401,
                        "name": "BAD_CREDENTIALS",
                        "message": "아이디 또는 비밀번호가 올바르지 않습니다."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 404,
                        "name": "USER_NOT_FOUND",
                        "message": "해당 유저를 찾을 수 없습니다."
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest);

    @SecurityRequirements(value = {})
    @Operation(summary = "토큰 재발급", description = "리프레쉬 토큰으로 액세스/리프레쉬 토큰 재발급 (rotation)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "재발급 성공",
                    content = @Content(
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject("""
                    {
                        "userId": 123,
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "tokenType": "Bearer",
                        "accessTokenExpiresAt": "2026-04-30T13:00:00",
                        "refreshTokenExpiresAt": "2026-05-30T13:00:00"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401", description = "유효하지 않거나 만료된 리프레쉬 토큰",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "유효하지 않은 토큰", value = """
                                    {
                                        "code": 401,
                                        "name": "REFRESH_TOKEN_NOT_FOUND",
                                        "message": "유효하지 않은 리프레쉬 토큰입니다."
                                    }
                                    """),
                                    @ExampleObject(name = "만료된 토큰", value = """
                                    {
                                        "code": 401,
                                        "name": "REFRESH_TOKEN_EXPIRED",
                                        "message": "리프레쉬 토큰이 만료되었습니다. 다시 로그인해주세요."
                                    }
                                    """)
                            }
                    )
            )
    })
    ResponseEntity<LoginResponse> refresh(@Valid @RequestBody TokenRefreshRequest request);

    @SecurityRequirements(value = {})
    @Operation(summary = "로그아웃", description = "리프레쉬 토큰 무효화")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    ResponseEntity<Void> logout(@Valid @RequestBody TokenRefreshRequest request);

    @Operation(summary = "Firebase 토큰 발급", description = "Firebase 실시간 채팅용 커스텀 토큰 발급")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "토큰 발급 성공",
                    content = @Content(
                            schema = @Schema(implementation = FirebaseTokenResponse.class),
                            examples = @ExampleObject("""
                    {
                        "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 401,
                        "name": "JWT_ENTRY_POINT",
                        "message": "로그인이 필요합니다."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500", description = "Firebase 서비스 오류",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 500,
                        "name": "FIREBASE_SERVICE_ERROR",
                        "message": "서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                    }
                    """
                            )
                    )
            )
    })
    ResponseEntity<FirebaseTokenResponse> getFirebaseToken(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) throws FirebaseAuthException;

    @SecurityRequirements(value = {})
    @Operation(summary = "Google OAuth 인증", description = """
            [1단계] Google Sign-In SDK로 획득한 ID Token을 전송합니다.
            - isNewUser: false → loginData의 accessToken/refreshToken으로 로그인 처리
            - isNewUser: true  → tempToken을 저장 후 프로필 입력 화면으로 이동, /auth/oauth/google/complete 호출 시 사용
            tempToken 유효시간: 10분""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "인증 성공",
                    content = @Content(
                            schema = @Schema(implementation = GoogleLoginResponse.class),
                            examples = {
                                    @ExampleObject(name = "기존 유저", value = """
                                    {
                                        "isNewUser": false,
                                        "tempToken": null,
                                        "loginData": {
                                            "userId": 123,
                                            "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                            "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                            "tokenType": "Bearer",
                                            "accessTokenExpiresAt": "2026-04-30T13:00:00",
                                            "refreshTokenExpiresAt": "2026-05-30T13:00:00"
                                        }
                                    }
                                    """),
                                    @ExampleObject(name = "신규 유저", value = """
                                    {
                                        "isNewUser": true,
                                        "tempToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "loginData": null
                                    }
                                    """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401", description = "유효하지 않은 Google ID Token",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 401,
                        "name": "INVALID_OAUTH_TOKEN",
                        "message": "유효하지 않은 OAuth 토큰입니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<GoogleLoginResponse> googleAuthenticate(@Valid @RequestBody GoogleLoginRequest request);

    @SecurityRequirements(value = {})
    @Operation(summary = "Google OAuth 회원가입 완료", description = """
            [2단계] 신규 유저 전용. 1단계에서 받은 tempToken과 프로필 정보를 함께 전송합니다.
            - tempToken 만료(10분) 시 TEMP_TOKEN_EXPIRED → 앱에서 1단계부터 재시도 필요
            성공 시 accessToken/refreshToken 발급, 로그인 처리와 동일하게 저장""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "회원가입 또는 로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject("""
                    {
                        "userId": 123,
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "tokenType": "Bearer",
                        "accessTokenExpiresAt": "2026-04-30T13:00:00",
                        "refreshTokenExpiresAt": "2026-05-30T13:00:00"
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 400,
                        "name": "INVALID_INPUT_VALUE",
                        "message": "잘못된 입력입니다."
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401", description = "임시 토큰 만료 또는 유효하지 않음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "임시 토큰 만료", value = """
                                    {
                                        "code": 401,
                                        "name": "TEMP_TOKEN_EXPIRED",
                                        "message": "임시 토큰이 만료되었습니다. 다시 구글 로그인을 시도해주세요."
                                    }
                                    """),
                                    @ExampleObject(name = "유효하지 않은 토큰", value = """
                                    {
                                        "code": 401,
                                        "name": "INVALID_OAUTH_TOKEN",
                                        "message": "유효하지 않은 OAuth 토큰입니다."
                                    }
                                    """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409", description = "이미 가입된 계정",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 409,
                        "name": "DUPLICATED_USER",
                        "message": "이미 존재하는 유저입니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<LoginResponse> googleRegister(@Valid @RequestBody GoogleRegisterRequest request);
}
