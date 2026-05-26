package com.everybuddy.global.swagger;

import com.everybuddy.domain.translate.dto.SpeechTranslateResponse;
import com.everybuddy.domain.translate.dto.TextTranslateRequest;
import com.everybuddy.domain.translate.dto.TextTranslateResponse;
import com.everybuddy.domain.translate.dto.TtsRequest;
import com.everybuddy.domain.translate.dto.VideoTranslateResponse;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "번역 API", description = "텍스트 및 음성 번역 기능")
public interface TranslateApiSpecification {

    @Operation(summary = "텍스트 번역", description = "텍스트를 사용자의 주 언어로 번역합니다. 원본 언어는 모델이 자동 감지합니다.")
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
            @Valid @RequestBody(required = true) TextTranslateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(
            summary = "음성 번역",
            description = "오디오 파일을 업로드하면 음성을 인식하고 사용자의 주 언어로 번역합니다. 원본 언어는 모델이 자동 감지합니다.",
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
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "TTS (텍스트 → 음성)", description = "텍스트를 입력받아 WAV 오디오를 반환합니다. language와 voice는 선택 항목입니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "TTS 성공 (audio/wav)",
                    content = @Content(mediaType = "audio/wav", schema = @Schema(type = "string", format = "binary"))
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
                                            "text": "변환할 텍스트를 입력해주세요."
                                        }
                                    }
                                    """),
                                    @ExampleObject(name = "텍스트 길이 초과", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_INPUT_VALUE",
                                        "message": "잘못된 입력입니다.",
                                        "errors": {
                                            "text": "텍스트는 최대 500자까지 입력할 수 있습니다."
                                        }
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
                    responseCode = "502", description = "TTS 모델 오류",
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
                    responseCode = "504", description = "TTS 요청 타임아웃",
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
    ResponseEntity<byte[]> tts(
            @Valid @RequestBody(required = true) TtsRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(
            summary = "영상 번역",
            description = """
                    영상 파일을 업로드하면 VAD로 음성 구간을 분리하고 사용자의 주 언어로 번역합니다.
                    원본 언어는 모델이 자동 감지합니다. 음성 구간이 없으면 segments 빈 배열로 응답합니다.
                    """,
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = VideoTranslateMultipart.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "번역 성공",
                    content = @Content(schema = @Schema(implementation = VideoTranslateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "잘못된 입력",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "지원하지 않는 영상 형식", value = """
                                    {
                                        "code": 400,
                                        "name": "INVALID_VIDEO_FORMAT",
                                        "message": "지원하지 않는 영상 형식입니다. (mp4, mov)"
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
                        "name": "VIDEO_FILE_TOO_LARGE",
                        "message": "영상 파일 크기가 제한을 초과했습니다. (최대 50MB)"
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500", description = "영상 변환 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 500,
                        "name": "VIDEO_CONVERT_FAILED",
                        "message": "영상에서 오디오 추출 중 오류가 발생했습니다."
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
    ResponseEntity<VideoTranslateResponse> translateVideo(
            @RequestPart MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );
}
