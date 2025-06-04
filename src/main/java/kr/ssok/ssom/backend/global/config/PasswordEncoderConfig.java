package kr.ssok.ssom.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화 설정
 * SecurityConfig와 분리하여 순환 의존성 문제 해결
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 암호화 방식을 사용하는 PasswordEncoder Bean 생성
     * 
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}