package kr.ssok.ssom.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE(Server-Sent Events) 전용 설정 클래스
 * SSE 연결 시 발생하는 보안 컨텍스트 관련 문제를 해결하기 위한 설정
 */
@Configuration
public class SseConfig {

    /**
     * SSE 전용 비동기 TaskExecutor
     * Security Context가 비동기 스레드로 전파되도록 설정
     */
    @Bean("sseAsyncTaskExecutor")
    public DelegatingSecurityContextAsyncTaskExecutor sseAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sse-security-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    /**
     * SSE Emitter 기본 설정을 위한 Bean
     */
    @Bean
    public SseEmitter.SseEventBuilder defaultSseEventBuilder() {
        return SseEmitter.event()
                .name("heartbeat")
                .data("ping");
    }
}
