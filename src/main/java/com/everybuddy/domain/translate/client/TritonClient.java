package com.everybuddy.domain.translate.client;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TritonClient {

    private static final String T2TT_MODEL = "/v2/models/gemma_t2tt/infer";
    private static final String S2TT_MODEL = "/v2/models/gemma_s2tt/infer";

    private static final List<String> T2TT_OUTPUTS = List.of(
            "SOURCE_TEXT", "SOURCE_LANGUAGE", "TRANSLATED_TEXT",
            "TARGET_LANGUAGE", "RAW_RESPONSE", "INFERENCE_SECONDS"
    );
    private static final List<String> S2TT_OUTPUTS = List.of(
            "SOURCE_TEXT", "SOURCE_LANGUAGE", "TRANSLATED_TEXT",
            "TARGET_LANGUAGE", "RAW_RESPONSE", "INFERENCE_SECONDS"
    );

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public TritonClient(@Value("${triton.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restTemplate = new RestTemplate(factory);
    }

    public String translateText(String text, String sourceLang, String targetLang) {
        TritonInferRequest request = TritonInferRequest.builder()
                .inputs(List.of(
                        textInput("TEXT", text),
                        textInput("SOURCE_LANGUAGE", sourceLang != null ? sourceLang : ""),
                        textInput("TARGET_LANGUAGE", targetLang)
                ))
                .outputs(buildOutputs(T2TT_OUTPUTS))
                .build();

        TritonInferResponse response = call(T2TT_MODEL, request);

        String result = response.extractString("TRANSLATED_TEXT");
        if (result == null || result.isBlank()) {
            throw new CustomException(ErrorCode.MODEL_ERROR);
        }
        return result;
    }

    public SpeechTranslationResult translateSpeech(byte[] audioBytes, String targetLang) {
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

        TritonInferRequest request = TritonInferRequest.builder()
                .inputs(List.of(
                        textInput("AUDIO_BYTES", base64Audio),
                        textInput("TARGET_LANGUAGE", targetLang)
                ))
                .outputs(buildOutputs(S2TT_OUTPUTS))
                .build();

        TritonInferResponse response = call(S2TT_MODEL, request);

        String sourceText = response.extractString("SOURCE_TEXT");
        String translatedText = response.extractString("TRANSLATED_TEXT");
        String sourceLanguage = response.extractString("SOURCE_LANGUAGE");

        if (sourceText == null || sourceText.isBlank()
                || translatedText == null || translatedText.isBlank()) {
            throw new CustomException(ErrorCode.MODEL_ERROR);
        }
        return new SpeechTranslationResult(sourceText, translatedText, sourceLanguage);
    }

    private TritonInferResponse call(String path, TritonInferRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TritonInferRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<TritonInferResponse> response = restTemplate.postForEntity(
                    baseUrl + path, entity, TritonInferResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Triton 오류 응답: {} - {}", e.getStatusCode().value(), e.getMessage());
            if (e.getStatusCode().value() == 503) {
                throw new CustomException(ErrorCode.MODEL_UNAVAILABLE);
            }
            throw new CustomException(ErrorCode.MODEL_ERROR);
        } catch (ResourceAccessException e) {
            Throwable cause = findRootCause(e);
            if (cause instanceof SocketTimeoutException ste) {
                String msg = ste.getMessage() != null ? ste.getMessage().toLowerCase() : "";
                if (msg.contains("read timed out") || msg.contains("read timeout")) {
                    log.error("Triton 응답 타임아웃: {}", e.getMessage());
                    throw new CustomException(ErrorCode.MODEL_TIMEOUT);
                }
            }
            log.error("Triton 연결 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.MODEL_UNAVAILABLE);
        }
    }

    private TritonInferRequest.Input textInput(String name, String value) {
        return TritonInferRequest.Input.builder()
                .name(name)
                .shape(List.of(1))
                .datatype("BYTES")
                .data(List.of(value))
                .build();
    }

    private List<TritonInferRequest.Output> buildOutputs(List<String> names) {
        return names.stream()
                .map(name -> TritonInferRequest.Output.builder()
                        .name(name)
                        .parameters(Map.of("binary_data", false))
                        .build())
                .toList();
    }

    private Throwable findRootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public record SpeechTranslationResult(
            String sourceText,
            String translatedText,
            String sourceLanguage
    ) {}
}
