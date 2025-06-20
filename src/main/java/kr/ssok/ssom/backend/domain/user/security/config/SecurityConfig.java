package kr.ssok.ssom.backend.domain.user.security.config;

import kr.ssok.ssom.backend.domain.user.security.filter.JwtAuthenticationFilter;
import kr.ssok.ssom.backend.domain.user.security.handler.JwtAuthenticationEntryPoint;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserService userService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 화이트리스트 경로 - 인증이 필요없는 경로들
     */
    private static final List<String> WHITELIST_PATHS = List.of(
            "/api/users/login",
            "/api/users/refresh", 
            "/api/users/signup",
            "/api/users/phone",
            "/api/users/phone/verify",
            "/actuator/prometheus",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error",
            "/actuator/health/readiness",
            "/actuator/health/liveness",
            "/actuator/info",
            "/actuator",
            "/actuator/prometheus",
            "/api/alert/grafana",
            "/api/alert/opensearch",
            "/api/alert/opensearch/sync",
            "/api/alert/issue",
            "/api/alert/devops",
            "/api/issue/webhook/github",
            "/",
            "/api/logging/opensearch"
    );

    /**
     * 비동기 처리를 위한 Security Context 전파 설정
     */
    static {
        // 비동기 처리 시 Security Context 전파를 위한 전략 설정
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    /**
     * JWT 인증 필터를 Bean으로 등록 (순환참조 방지)
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate, userService, WHITELIST_PATHS);
    }

    /**
     * 비동기 처리를 위한 DelegatingSecurityContextAsyncTaskExecutor 설정
     */
    @Bean
    public DelegatingSecurityContextAsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sse-");
        executor.initialize();
        
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    /**
     * SSE를 위한 추가 비동기 처리 설정
     */
    @Bean
    public DelegatingSecurityContextAsyncTaskExecutor sseTaskExecutor() {
        return new DelegatingSecurityContextAsyncTaskExecutor(
                new SimpleAsyncTaskExecutor("sse-emitter-")
        );
    }

    /**
     * Spring Security 필터 체인 구성
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
                
                // 예외 처리 설정 - SSE 응답 커밋 상태 고려
                .exceptionHandling(exception -> 
                    exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                
                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(WHITELIST_PATHS.toArray(new String[0])).permitAll()
                    .anyRequest().authenticated()
                )
                
                // JWT 인증 필터 추가
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                
                // 비동기 요청 지원 활성화 (SSE 포함)
                .headers(headers -> headers
                    .frameOptions(frameOptions -> frameOptions.sameOrigin())
                    .httpStrictTransportSecurity(hstsConfig -> hstsConfig.disable())
                )
                
                .build();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // TODO: Prod에서는 특정 도메인만 허용하도록 변경 필요
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Refresh-Token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}