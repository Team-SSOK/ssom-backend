package kr.ssok.ssom.backend.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper 설정
 */
@Configuration
public class JacksonConfig {
    
    /**
     * ObjectMapper Bean 생성
     * GitHub Webhook JSON 파싱 등에 사용
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Java 8 Time API 지원
        objectMapper.registerModule(new JavaTimeModule());
        
        // 기본 camelCase 유지 (GitHub Webhook용으로만 snake_case 사용하도록 분리)
        // objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        
        // 알려지지 않은 속성 무시 (GitHub API 응답에 새로운 필드가 추가되어도 오류 없음)
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return objectMapper;
    }
    
    /**
     * GitHub Webhook 전용 ObjectMapper
     * snake_case 속성명 처리용
     */
    @Bean("githubObjectMapper")
    public ObjectMapper githubObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Java 8 Time API 지원
        objectMapper.registerModule(new JavaTimeModule());
        
        // snake_case 속성명을 camelCase로 자동 변환 (GitHub Webhook용)
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        
        // 알려지지 않은 속성 무시
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return objectMapper;
    }
}
