package com.everybuddy.domain.translate.client;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class TritonClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String T2TT_MODEL          = "/v2/models/gemma_t2tt/infer";
    private static final String S2TT_MODEL          = "/v2/models/gemma_s2tt/infer";
    private static final String V2TT_MODEL          = "/v2/models/gemma_v2tt/infer";
    private static final String TTS_MODEL           = "/v2/models/Supertonic_tts/infer";
    private static final String V2TT_PREPARE_MODEL  = "/v2/models/gemma_v2tt_prepare/infer";
    private static final String V2TT_SEGMENT_MODEL  = "/v2/models/gemma_v2tt_segment/infer";
    private static final String V2TT_CLEANUP_MODEL  = "/v2/models/gemma_v2tt_cleanup/infer";

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

    private TritonInferResponse callS2TTBinary(byte[] audioBytes, String targetLang) {
        return callS2TTBinaryCore(audioBytes, targetLang);
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
            throw new CustomException(ErrorCode.MODEL_ERROR, e);
        }
    }

    public record SpeechTranslationResult(String sourceText, String translatedText) {}

    public record VideoPrepareResult(String jobId, String segmentsJson) {}

    /**
     * gemma_v2tt 전용 영상 번역.
     * MP4/MOV 원본 바이트와 목표 언어 코드를 Triton에 전송하고
     * SEGMENTS_JSON (raw JSON 배열 문자열)을 반환한다.
     * 무음 영상의 경우 Triton이 [{index:0, total_segments:0, ...}] 형태의 더미 세그먼트를 반환하며,
     * 이는 호출 측(TranslateService)에서 total_segments==0 조건으로 필터링한다.
     */
    public String translateVideoSpeech(byte[] videoBytes, String targetLang) {
        byte[] langBytes = targetLang.getBytes(StandardCharsets.UTF_8);
        int audioPayloadSize = 4 + videoBytes.length;
        int langPayloadSize  = 4 + langBytes.length;

        String jsonHeader = String.format(
                "{\"inputs\":[" +
                "{\"name\":\"AUDIO_BYTES\",\"shape\":[1],\"datatype\":\"BYTES\",\"parameters\":{\"binary_data_size\":%d}}," +
                "{\"name\":\"TARGET_LANGUAGE\",\"shape\":[1],\"datatype\":\"BYTES\",\"parameters\":{\"binary_data_size\":%d}}" +
                "],\"outputs\":[" +
                "{\"name\":\"SEGMENTS_JSON\",\"parameters\":{\"binary_data\":false}}" +
                "]}",
                audioPayloadSize, langPayloadSize);

        byte[] jsonBytes = jsonHeader.getBytes(StandardCharsets.UTF_8);

        ByteBuffer binary = ByteBuffer.allocate(audioPayloadSize + langPayloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        binary.putInt(videoBytes.length);
        binary.put(videoBytes);
        binary.putInt(langBytes.length);
        binary.put(langBytes);

        byte[] body = new byte[jsonBytes.length + binary.capacity()];
        System.arraycopy(jsonBytes, 0, body, 0, jsonBytes.length);
        System.arraycopy(binary.array(), 0, body, jsonBytes.length, binary.capacity());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("Inference-Header-Content-Length", String.valueOf(jsonBytes.length));

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        TritonInferResponse response = execute(
                () -> restTemplate.postForEntity(baseUrl + V2TT_MODEL, entity, TritonInferResponse.class));
        return response.getOutputValue("SEGMENTS_JSON");
    }

    // ── V2TT 스트리밍 3종 엔드포인트 ────────────────────────────────────────────

    /**
     * gemma_v2tt_prepare: 영상 bytes → ffmpeg decode + VAD → job_id + timestamps 반환.
     * VIDEO_BYTES가 최대 50 MB이므로 기존 translateVideoSpeech와 동일한 binary HTTP extension 사용.
     */
    public VideoPrepareResult prepareVideo(byte[] videoBytes) {
        int videoPayloadSize = 4 + videoBytes.length;

        String jsonHeader = String.format(
                "{\"inputs\":[" +
                "{\"name\":\"VIDEO_BYTES\",\"shape\":[1],\"datatype\":\"BYTES\",\"parameters\":{\"binary_data_size\":%d}}" +
                "],\"outputs\":[" +
                "{\"name\":\"JOB_ID\",\"parameters\":{\"binary_data\":false}}," +
                "{\"name\":\"SEGMENTS_JSON\",\"parameters\":{\"binary_data\":false}}" +
                "]}",
                videoPayloadSize);

        byte[] jsonBytes = jsonHeader.getBytes(StandardCharsets.UTF_8);

        ByteBuffer binary = ByteBuffer.allocate(videoPayloadSize).order(ByteOrder.LITTLE_ENDIAN);
        binary.putInt(videoBytes.length);
        binary.put(videoBytes);

        byte[] body = new byte[jsonBytes.length + binary.capacity()];
        System.arraycopy(jsonBytes, 0, body, 0, jsonBytes.length);
        System.arraycopy(binary.array(), 0, body, jsonBytes.length, binary.capacity());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("Inference-Header-Content-Length", String.valueOf(jsonBytes.length));

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
        TritonInferResponse response = execute(
                () -> restTemplate.postForEntity(baseUrl + V2TT_PREPARE_MODEL, entity, TritonInferResponse.class));

        return new VideoPrepareResult(
                response.getOutputValue("JOB_ID"),
                response.getOutputValue("SEGMENTS_JSON"));
    }

    /**
     * gemma_v2tt_segment: job_id + 구간 정보 → 단일 segment ASR + 번역 결과(SEGMENT_JSON) 반환.
     * 입력이 모두 소용량 문자열이므로 일반 JSON 경로 사용.
     * sourceLang 이 null 이거나 blank 이면 입력에서 제외 → Gemma 자동 감지.
     */
    public String translateSegment(String jobId, String segmentIndex,
                                   String startSeconds, String endSeconds,
                                   String targetLang, String sourceLang) {
        List<TritonInferRequest.Input> inputs = new ArrayList<>();
        inputs.add(textInput("JOB_ID", jobId));
        inputs.add(textInput("SEGMENT_INDEX", segmentIndex));
        inputs.add(textInput("START_SECONDS", startSeconds));
        inputs.add(textInput("END_SECONDS", endSeconds));
        inputs.add(textInput("TARGET_LANGUAGE", targetLang));
        if (sourceLang != null && !sourceLang.isBlank()) {
            inputs.add(textInput("SOURCE_LANGUAGE", sourceLang));
        }

        TritonInferRequest request = TritonInferRequest.builder()
                .inputs(inputs)
                .outputs(List.of(output("SEGMENT_JSON")))
                .build();

        TritonInferResponse response = call(V2TT_SEGMENT_MODEL, request);
        return response.getOutputValue("SEGMENT_JSON");
    }

    /**
     * gemma_v2tt_cleanup: job_id 에 해당하는 임시 오디오 파일 삭제.
     * cleanup 실패는 치명적이지 않으므로 예외를 전파하지 않고 경고 로그만 남긴다.
     */
    public void cleanupJob(String jobId) {
        try {
            TritonInferRequest request = TritonInferRequest.builder()
                    .inputs(List.of(textInput("JOB_ID", jobId)))
                    .outputs(List.of(output("STATUS")))
                    .build();
            call(V2TT_CLEANUP_MODEL, request);
        } catch (Exception e) {
            log.warn("[v2tt] cleanup failed for job {}: {}", jobId, e.getMessage());
        }
    }

    // Triton binary HTTP extension: JSON header + raw bytes payload (gemma_s2tt 음성 번역 전용)
    private TritonInferResponse callS2TTBinaryCore(byte[] audioBytes, String targetLang) {
        byte[] langBytes = targetLang.getBytes(StandardCharsets.UTF_8);
        int audioPayloadSize = 4 + audioBytes.length;
        int langPayloadSize  = 4 + langBytes.length;

        // Each BYTES element: 4-byte little-endian length prefix + raw bytes
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
