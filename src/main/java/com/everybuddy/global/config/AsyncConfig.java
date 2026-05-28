package com.everybuddy.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * V2TT 스트리밍 전용 스레드풀.
     * 동시 스트리밍 요청당 최대 4개(outer 1 + segment 병렬 3) 스레드 사용.
     * corePoolSize 6 → 동시 요청 1~2개 처리, maxPoolSize 12 → 순간 burst 대응.
     */
    @Bean(name = "v2ttExecutor")
    public Executor v2ttExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("v2tt-");
        executor.initialize();
        return executor;
    }
}
