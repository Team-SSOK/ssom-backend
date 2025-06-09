package kr.ssok.ssom.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Callable;

/**
 * Spring MVC 설정 - 비동기 처리 및 CORS 설정
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 비동기 처리를 위한 TaskExecutor 설정
     */
    @Bean
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-sse-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Security Context를 전파하는 비동기 TaskExecutor
     */
    @Bean
    public DelegatingSecurityContextAsyncTaskExecutor securityContextAsyncTaskExecutor() {
        return new DelegatingSecurityContextAsyncTaskExecutor(asyncTaskExecutor());
    }

    /**
     * 비동기 요청 처리 설정 - SSE 최적화
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(securityContextAsyncTaskExecutor());
        configurer.setDefaultTimeout(120000); // 2분으로 확장 (SSE 장시간 연결 고려)
        
        // 비동기 요청 인터셉터 등록
        configurer.registerCallableInterceptors(new SseCallableProcessingInterceptor());
        configurer.registerDeferredResultInterceptors(new SseDeferredResultProcessingInterceptor());
    }

    /**
     * SSE 전용 Callable 인터셉터
     */
    private static class SseCallableProcessingInterceptor implements CallableProcessingInterceptor {
        @Override
        public <T> void beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) throws Exception {
            // SSE 요청 시 특별 처리 로직 (필요시 확장)
        }

        @Override
        public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
            // SSE 완료 후 정리 작업 (필요시 확장)
        }
    }

    /**
     * SSE 전용 DeferredResult 인터셉터 
     */
    private static class SseDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor {
        @Override
        public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
            // SSE 타임아웃 처리 최적화
            return true; // true 반환하여 기본 타임아웃 처리 수행
        }
    }

    /**
     * CORS 설정 (추가 보강)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Refresh-Token")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
