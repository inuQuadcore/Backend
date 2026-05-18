package com.everybuddy.domain.translate.client;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class TritonClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String T2TT_MODEL = "/v2/models/gemma_t2tt/infer";
    private static final String S2TT_MODEL = "/v2/models/gemma_s2tt/infer";
    private static final String TTS_MODEL = "/v2/models/Supertonic_tts/infer";

    private static final String OUTPUT_TRANSLATED_TEXT = "TRANSLATED_TEXT";
    private static final String OUTPUT_SOURCE_TEXT = "SOURCE_TEXT";
    private static final String OUTPUT_AUDIO_BYTES = "AUDIO_BYTES";

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
        TritonInferResponse response = callS2TTBinary(audioBytes, targetLang);

        String sourceText = response.getOutputValue(OUTPUT_SOURCE_TEXT);
        String translatedText = response.getOutputValue(OUTPUT_TRANSLATED_TEXT);
        if (sourceText.isBlank() || translatedText.isBlank()) {
            throw new CustomException(ErrorCode.MODEL_ERROR);
        }
        return new SpeechTranslationResult(sourceText, translatedText);
    }

    // Triton binary HTTP extension: JSON header + raw bytes payload.
    // JSON protocol encodes BYTES as base64 strings, which ffmpeg cannot decode.
    // Binary protocol delivers raw audio bytes directly to the model.
    private TritonInferResponse callS2TTBinary(byte[] audioBytes, String targetLang) {
        byte[] langBytes = targetLang.getBytes(StandardCharsets.UTF_8);
        int audioPayloadSize = 4 + audioBytes.length;
        int langPayloadSize = 4 + langBytes.length;

        String jsonHeader = String.format(
                "{\"inputs\":[" +
                "{\"name\":\"AUDIO_BYTES\",\"shape\":[1],\"datatype\":\"BYTES\",\"parameters\":{\"binary_data_size\":%d}}," +
                "{\"name\":\"TARGET_LANGUAGE\",\"shape\":[1],\"datatype\":\"BYTES\",\"parameters\":{\"binary_data_size\":%d}}" +
                "],\"outputs\":[" +
                "{\"name\":\"SOURCE_TEXT\",\"parameters\":{\"binary_data\":false}}," +
                "{\"name\":\"TRANSLATED_TEXT\",\"parameters\":{\"binary_data\":false}}" +
                "]}",
                audioPayloadSize, langPayloadSize
        );

        byte[] jsonBytes = jsonHeader.getBytes(StandardCharsets.UTF_8);

        // Each BYTES element: 4-byte little-endian length prefix + raw bytes
        ByteBuffer binary = ByteBuffer.allocate(audioPayloadSize + langPayloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        binary.putInt(audioBytes.length);
        binary.put(audioBytes);
        binary.putInt(langBytes.length);
        binary.put(langBytes);

        byte[] body = new byte[jsonBytes.length + binary.capacity()];
        System.arraycopy(jsonBytes, 0, body, 0, jsonBytes.length);
        System.arraycopy(binary.array(), 0, body, jsonBytes.length, binary.capacity());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("Inference-Header-Content-Length", String.valueOf(jsonBytes.length));

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        return execute(() -> restTemplate.postForEntity(baseUrl + S2TT_MODEL, entity, TritonInferResponse.class));
    }

    public byte[] synthesizeSpeech(String text, String language, String voice) {
        List<TritonInferRequest.Input> inputs = new ArrayList<>();
        inputs.add(textInput("TEXT_INPUT", text));
        if (language != null && !language.isBlank()) {
            inputs.add(textInput("LANGUAGE", language));
        }
        if (voice != null && !voice.isBlank()) {
            inputs.add(textInput("VOICE", voice));
        }

        TritonInferRequest request = TritonInferRequest.builder()
                .inputs(inputs)
                .outputs(List.of(output(OUTPUT_AUDIO_BYTES)))
                .build();

        HttpHeaders reqHeaders = new HttpHeaders();
        reqHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TritonInferRequest> entity = new HttpEntity<>(request, reqHeaders);

        ResponseEntity<byte[]> rawResponse = executeRaw(
                () -> restTemplate.exchange(baseUrl + TTS_MODEL, HttpMethod.POST, entity, byte[].class)
        );

        byte[] body = rawResponse.getBody();
        if (body == null || body.length == 0) {
            throw new CustomException(ErrorCode.MODEL_ERROR);
        }

        // Triton binary HTTP: 응답 헤더에 JSON 헤더 길이가 있으면 그 이후가 오디오 raw bytes
        String inferHeaderLengthStr = rawResponse.getHeaders().getFirst("Inference-Header-Content-Length");
        if (inferHeaderLengthStr != null) {
            int headerLen = Integer.parseInt(inferHeaderLengthStr);
            byte[] audioBytes = Arrays.copyOfRange(body, headerLen, body.length);
            if (audioBytes.length == 0) {
                throw new CustomException(ErrorCode.MODEL_ERROR);
            }
            return audioBytes;
        }

        // JSON 모드: AUDIO_BYTES가 base64로 인코딩된 경우
        try {
            TritonInferResponse response = OBJECT_MAPPER.readValue(body, TritonInferResponse.class);
            String base64Audio = response.getOutputValue(OUTPUT_AUDIO_BYTES);
            return Base64.getDecoder().decode(base64Audio);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            // 모델이 raw 오디오 바이트를 JSON 응답에 직접 포함하는 경우:
            // WAV 파일 시그니처(RIFF....WAVE)를 탐색해 오디오 데이터 추출
            byte[] wav = extractWavFromBody(body);
            if (wav != null) {
                return wav;
            }
            throw new CustomException(ErrorCode.MODEL_ERROR, e);
        }
    }

    private static byte[] extractWavFromBody(byte[] body) {
        for (int i = 0; i <= body.length - 12; i++) {
            if (body[i] == 'R' && body[i + 1] == 'I' && body[i + 2] == 'F' && body[i + 3] == 'F'
                    && body[i + 8] == 'W' && body[i + 9] == 'A' && body[i + 10] == 'V' && body[i + 11] == 'E') {
                int riffSize = ((body[i + 4] & 0xFF))
                        | ((body[i + 5] & 0xFF) << 8)
                        | ((body[i + 6] & 0xFF) << 16)
                        | ((body[i + 7] & 0xFF) << 24);
                int end = i + 8 + riffSize;
                if (end > body.length) end = body.length;
                return Arrays.copyOfRange(body, i, end);
            }
        }
        return null;
    }

    public record SpeechTranslationResult(String sourceText, String translatedText) {}

    private TritonInferResponse call(String path, TritonInferRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TritonInferRequest> entity = new HttpEntity<>(request, headers);

        return execute(() -> restTemplate.postForEntity(baseUrl + path, entity, TritonInferResponse.class));
    }

    private TritonInferResponse execute(Supplier<ResponseEntity<TritonInferResponse>> supplier) {
        try {
            return supplier.get().getBody();
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 503) throw new CustomException(ErrorCode.MODEL_UNAVAILABLE, e);
            if (status == 400 || status == 404) throw new CustomException(ErrorCode.MODEL_REQUEST_INVALID, e);
            throw new CustomException(ErrorCode.MODEL_ERROR, e);
        } catch (ResourceAccessException e) {
            if (isReadTimeout(e)) throw new CustomException(ErrorCode.MODEL_TIMEOUT, e);
            throw new CustomException(ErrorCode.MODEL_UNAVAILABLE, e);
        }
    }

    private ResponseEntity<byte[]> executeRaw(Supplier<ResponseEntity<byte[]>> supplier) {
        try {
            return supplier.get();
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 503) throw new CustomException(ErrorCode.MODEL_UNAVAILABLE, e);
            if (status == 400 || status == 404) throw new CustomException(ErrorCode.MODEL_REQUEST_INVALID, e);
            throw new CustomException(ErrorCode.MODEL_ERROR, e);
        } catch (ResourceAccessException e) {
            if (isReadTimeout(e)) throw new CustomException(ErrorCode.MODEL_TIMEOUT, e);
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
