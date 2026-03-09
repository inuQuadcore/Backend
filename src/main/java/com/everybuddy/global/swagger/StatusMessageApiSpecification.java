package com.everybuddy.global.swagger;

import com.everybuddy.domain.statusmessage.dto.CreateStatusMessageRequest;
import com.everybuddy.global.exception.ErrorResponse;
import com.everybuddy.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "상태메시지 API", description = "상태메시지 관련 기능")
public interface StatusMessageApiSpecification {

    @Operation(summary = "상태메시지 작성", description = "상태메시지를 작성합니다. 인당 하나만 작성 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태메시지 작성 성공"),
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
                    responseCode = "409", description = "이미 상태메시지 존재",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("""
                    {
                        "code": 409,
                        "name": "STATUS_MESSAGE_ALREADY_EXISTS",
                        "message": "이미 상태메시지가 존재합니다."
                    }
                    """)
                    )
            )
    })
    ResponseEntity<Void> createStatusMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CreateStatusMessageRequest request
    );
}
