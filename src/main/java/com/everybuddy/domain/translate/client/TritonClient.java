package com.everybuddy.domain.translate.client;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
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

@Component
public class TritonClient {

    private static final String T2TT_MODEL = "/v2/models/gemma_t2tt/infer";
    private static final String S2TT_MODEL = "/v2/models/gemma_s2tt/infer";

    private static final String OUTPUT_TRANSLATED_TEXT = "TRANSLATED_TEXT";
    private static final String OUTPUT_SOURCE_TEXT = "SOURCE_TEXT";

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public TritonClient(@Value("${triton.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restTemplate = new RestTemplate(factory);
    }

    public String translateText(String text, String targetLang) {
        TritonInferRequest request = TritonInferRequest.builder()
                .inputs(List.of(
                        textInput("TEXT", text),
                        textInput("SOURCE_LANGUAGE", ""),
                        textInput("TARGET_LANGUAGE", targetLang)
                ))
                .outputs(List.of(output(OUTPUT_TRANSLATED_TEXT)))
                .build();

        TritonInferResponse response = call(T2TT_MODEL, request);

        String result = response.getOutputValue(OUTPUT_TRANSLATED_TEXT);
        if (result.isBlank()) {
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
                .outputs(List.of(
                        output(OUTPUT_SOURCE_TEXT),
                        output(OUTPUT_TRANSLATED_TEXT)
                ))
                .build();

        TritonInferResponse response = call(S2TT_MODEL, request);

        String sourceText = response.getOutputValue(OUTPUT_SOURCE_TEXT);
        String translatedText = response.getOutputValue(OUTPUT_TRANSLATED_TEXT);
        if (sourceText.isBlank() || translatedText.isBlank()) {
            throw new CustomException(ErrorCode.MODEL_ERROR);
        }
        return new SpeechTranslationResult(sourceText, translatedText);
    }

    public record SpeechTranslationResult(String sourceText, String translatedText) {}

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
            int status = e.getStatusCode().value();
            if (status == 503) {
                throw new CustomException(ErrorCode.MODEL_UNAVAILABLE, e);
            }
            if (status == 400 || status == 404) {
                throw new CustomException(ErrorCode.MODEL_REQUEST_INVALID, e);
            }
            throw new CustomException(ErrorCode.MODEL_ERROR, e);
        } catch (ResourceAccessException e) {
            if (isReadTimeout(e)) {
                throw new CustomException(ErrorCode.MODEL_TIMEOUT, e);
            }
            throw new CustomException(ErrorCode.MODEL_UNAVAILABLE, e);
        }
    }

    private boolean isReadTimeout(ResourceAccessException e) {
        return e.getCause() instanceof SocketTimeoutException ste
                && ste.getMessage() != null
                && ste.getMessage().toLowerCase().contains("read timed out");
    }

    private TritonInferRequest.Input textInput(String name, String value) {
        return TritonInferRequest.Input.builder()
                .name(name)
                .shape(List.of(1))
                .datatype("BYTES")
                .data(List.of(value))
                .build();
    }

    private TritonInferRequest.Output output(String name) {
        return TritonInferRequest.Output.builder()
                .name(name)
                .parameters(Map.of("binary_data", false))
                .build();
    }

}
