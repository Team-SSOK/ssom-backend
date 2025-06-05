package kr.ssok.ssom.backend.global.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,   // 연결 타임아웃 10초
                60, TimeUnit.SECONDS,    // 읽기 타임아웃 60초 (LLM 처리 시간 고려)
                true
        );
    }
}
