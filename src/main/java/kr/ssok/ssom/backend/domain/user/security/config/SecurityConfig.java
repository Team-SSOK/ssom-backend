package kr.ssok.ssom.backend.domain.user.security.config;

import kr.ssok.ssom.backend.domain.user.security.filter.JwtAuthenticationFilter;
import kr.ssok.ssom.backend.domain.user.security.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private List<String> whitelist = List.of(
            "/api/users/login",
            "/api/users/refresh", 
            "/api/users/signup",
            "/api/users/phone",
            "/api/users/phone/verify",
            "/actuator/prometheus",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 구성 (Servlet 기반)
     * JWT 인증 필터 추가 및 보안 정책 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // CSRF 비활성화 (JWT 사용으로 불필요)
                .csrf(AbstractHttpConfigurer::disable)
                
                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                
                // Form 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                
                // 세션 관리 정책: STATELESS (JWT 사용)
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 예외 처리 설정
                .exceptionHandling(exception -> 
                    exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                
                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(whitelist.toArray(new String[0])).permitAll()
                    .anyRequest().authenticated()
                )
                
                // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 배치)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                .build();
    }

    /**
     * CORS 설정 (Servlet 기반)
     * 크로스 오리진 리소스 공유 정책 구성
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // TODO: Prod에서는 특정 도메인만 허용하도록 변경 필요
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 모든 Origin 허용 (개발용)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Refresh-Token"));
        configuration.setAllowCredentials(true); // 인증 정보 포함 허용
        configuration.setMaxAge(3600L); // preflight 요청 캐시 시간 (1시간)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 화이트리스트 반환 (다른 클래스에서 사용할 수 있도록)
     */
    public List<String> getWhitelist() {
        return whitelist;
    }

    /**
     * 화이트리스트 설정 (application.yml에서 주입)
     */
    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
