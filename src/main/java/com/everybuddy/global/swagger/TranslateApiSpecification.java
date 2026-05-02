package com.everybuddy.global.swagger;

import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "번역 API", description = "텍스트 및 음성 번역 기능")
public interface TranslateApiSpecification {

    @Operation(summary = "텍스트 번역", description = "텍스트를 지정한 언어로 번역합니다. sourceLang을 생략하면 모델이 언어를 자동 감지합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "번역 성공",
                    content = @Content(schema = @Schema(implementation = TextTranslateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "텍스트 누락", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다.",
                                        "errors": {
                                            "text": "번역할 텍스트를 입력해주세요."
                                        }
                                    }
                                    """),
                                    @ExampleObject(name = "지원하지 않는 언어", value = """
                                    {
                                        "code": 400,
                                        "name": "UNSUPPORTED_LANGUAGE",
                                        "message": "지원하지 않는 언어 코드입니다."
                                    }
                                    """)
                            }
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
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "502", description = "번역 모델 오류",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "모델 처리 오류", value = """
                                    {
                                        "code": 502,
                                        "name": "MODEL_ERROR",
                                        "message": "번역 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                                    }
                                    """),
                                    @ExampleObject(name = "모델 서비스 불가", value = """
                                    {
                                        "code": 502,
                                        "name": "MODEL_UNAVAILABLE",
                                        "message": "번역 서비스를 현재 사용할 수 없습니다. 잠시 후 다시 시도해주세요."
                                    }
                                    """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "504", description = "번역 요청 타임아웃",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 504,
                        "name": "MODEL_TIMEOUT",
                        "message": "번역 요청 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<TextTranslateResponse> translateText(
            @Valid @RequestBody(required = true) TextTranslateRequest request
    );

    @Operation(
            summary = "음성 번역",
            description = "오디오 파일을 업로드하면 음성을 인식하고 지정한 언어로 번역합니다. 원본 언어는 모델이 자동 감지합니다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = SpeechTranslateMultipart.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "번역 성공",
                    content = @Content(schema = @Schema(implementation = SpeechTranslateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "지원하지 않는 언어", value = """
                                    {
                                        "code": 400,
                                        "name": "UNSUPPORTED_LANGUAGE",
                                        "message": "지원하지 않는 언어 코드입니다."
                                    }
                                    """),
                                    @ExampleObject(name = "지원하지 않는 오디오 형식", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_AUDIO_FORMAT",
                                        "message": "지원하지 않는 오디오 형식입니다."
                                    }
                                    """),
                                    @ExampleObject(name = "빈 파일", value = """
                                    {
                                        "code": 400,
                                        "name": "EMPTY_FILE",
                                        "message": "빈 파일은 업로드할 수 없습니다."
                                    }
                                    """)
                            }
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
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "413", description = "파일 크기 초과",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 413,
                        "name": "AUDIO_FILE_TOO_LARGE",
                        "message": "오디오 파일 크기가 제한을 초과했습니다. (최대 50MB)"
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "502", description = "번역 모델 오류",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "모델 처리 오류", value = """
                                    {
                                        "code": 502,
                                        "name": "MODEL_ERROR",
                                        "message": "번역 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                                    }
                                    """),
                                    @ExampleObject(name = "모델 서비스 불가", value = """
                                    {
                                        "code": 502,
                                        "name": "MODEL_UNAVAILABLE",
                                        "message": "번역 서비스를 현재 사용할 수 없습니다. 잠시 후 다시 시도해주세요."
                                    }
                                    """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "504", description = "번역 요청 타임아웃",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 504,
                        "name": "MODEL_TIMEOUT",
                        "message": "번역 요청 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<SpeechTranslateResponse> translateSpeech(
            @RequestPart MultipartFile file,
            @RequestParam String targetLang
    );
}
