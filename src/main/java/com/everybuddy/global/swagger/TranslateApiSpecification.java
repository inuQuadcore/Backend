package com.everybuddy.global.swagger;

import com.everybuddy.domain.translatemessage.dto.TranslateSpeechResponse;
import com.everybuddy.domain.translatemessage.dto.TranslateTextRequest;
import com.everybuddy.domain.translatemessage.dto.TranslateTextResponse;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "번역 API", description = "텍스트 번역(T2TT), 음성 번역(S2TT)")
public interface TranslateApiSpecification {

    @Operation(summary = "텍스트 번역", description = "텍스트를 입력 언어에서 출력 언어로 번역합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "번역 성공",
                    content = @Content(schema = @Schema(implementation = TranslateTextResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력 또는 지원하지 않는 언어",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 400,
                        "name": "TRANSLATE_UNSUPPORTED_LANGUAGE",
                        "message": "지원하지 않는 언어입니다."
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503", description = "번역 서비스 사용 불가",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 503,
                        "name": "TRANSLATE_SERVICE_UNAVAILABLE",
                        "message": "번역 서비스를 일시적으로 사용할 수 없습니다."
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "504", description = "번역 서버 타임아웃",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 504,
                        "name": "TRANSLATE_TIMEOUT",
                        "message": "번역 서버 응답 시간이 초과되었습니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<TranslateTextResponse> translateText(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TranslateTextRequest request
    );

    @Operation(summary = "음성 번역", description = "음성 파일을 텍스트로 전사한 후 번역합니다. 지원 형식: wav, mp3, ogg, webm, m4a (최대 10MB)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "번역 성공",
                    content = @Content(schema = @Schema(implementation = TranslateSpeechResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 파일 형식 또는 지원하지 않는 언어",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "지원하지 않는 오디오 형식", value = """
                                    {
                                        "code": 400,
                                        "name": "TRANSLATE_INVALID_AUDIO_FORMAT",
                                        "message": "wav, mp3, ogg, webm, m4a 형식만 지원합니다."
                                    }
                                    """),
                                    @ExampleObject(name = "파일 크기 초과", value = """
                                    {
                                        "code": 400,
                                        "name": "TRANSLATE_FILE_SIZE_EXCEEDED",
                                        "message": "파일 크기는 10MB 이하여야 합니다."
                                    }
                                    """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "503", description = "번역 서비스 사용 불가",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 503,
                        "name": "TRANSLATE_SERVICE_UNAVAILABLE",
                        "message": "번역 서비스를 일시적으로 사용할 수 없습니다."
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "504", description = "번역 서버 타임아웃",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 504,
                        "name": "TRANSLATE_TIMEOUT",
                        "message": "번역 서버 응답 시간이 초과되었습니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<TranslateSpeechResponse> translateSpeech(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String sourceLanguage,
            @RequestParam String targetLanguage,
            @RequestPart MultipartFile audio
    );
}
