package com.everybuddy.global.util;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * ffmpeg를 사용한 미디어 변환 유틸리티.
 * 절대 경로(@Value)를 사용해 PATH 의존성(SonarQube S4036) 을 제거합니다.
 */
@Component
public class FfmpegMediaConverter {

    private final String ffmpegPath;

    public FfmpegMediaConverter(@Value("${ffmpeg.path}") String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    /**
     * 동영상 바이트를 Triton S2TT 모델용 WAV(mono 16kHz)로 변환합니다.
     *
     * @param videoBytes mp4·mov 원본 바이트
     * @return PCM WAV 바이트
     * @throws CustomException VIDEO_CONVERT_FAILED
     */
    public byte[] convertVideoToWav(byte[] videoBytes) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath, "-i", "pipe:0",
                    "-vn",                          // 비디오 스트림 명시적 제외
                    "-ac", "1", "-ar", "16000",
                    "-f", "wav", "-loglevel", "error",
                    "pipe:1"
            );
            // stderr를 버려 ffmpeg 오류 메시지가 버퍼를 채워 프로세스가 블로킹되는 상황을 방지
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();

            // stdin 쓰기를 별도 스레드에서 처리 — stdout 읽기와 동시에 진행해야 데드락 방지
            Thread stdinWriter = new Thread(() -> {
                try (OutputStream stdin = process.getOutputStream()) {
                    stdin.write(videoBytes);
                } catch (IOException ignored) {}
            }, "ffmpeg-stdin-writer");
            stdinWriter.start();

            byte[] wavBytes = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(90, TimeUnit.SECONDS);
            stdinWriter.join(5_000);

            if (!finished || process.exitValue() != 0 || wavBytes.length == 0) {
                process.destroyForcibly();
                throw new CustomException(ErrorCode.VIDEO_CONVERT_FAILED);
            }
            return wavBytes;

        } catch (CustomException e) {
            throw e;
        } catch (InterruptedException e) {
            // 인터럽트 상태 복구 — 상위 레이어가 인터럽트를 인지할 수 있도록 보장
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.VIDEO_CONVERT_FAILED, e);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.VIDEO_CONVERT_FAILED, e);
        }
    }
}
