package kr.ssok.ssom.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
     * 비동기 요청 처리 설정
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(securityContextAsyncTaskExecutor());
        configurer.setDefaultTimeout(60000); // 60초
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
