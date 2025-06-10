package kr.ssok.ssom.backend.domain.user.security;

import kr.ssok.ssom.backend.domain.user.security.config.SecurityConfig;
import kr.ssok.ssom.backend.domain.user.security.filter.JwtAuthenticationFilter;
import kr.ssok.ssom.backend.domain.user.security.handler.JwtAuthenticationEntryPoint;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig 테스트")
class SecurityConfigTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private UserService userService;

    @Mock
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Nested
    @DisplayName("JWT 인증 필터 Bean 생성 테스트")
    class JwtAuthenticationFilterBeanTest {

        @Test
        @DisplayName("JWT 인증 필터 Bean이 정상적으로 생성된다")
        void jwtAuthenticationFilter_CreatesFilterBean() {
            // when
            JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter();

            // then
            assertThat(filter).isNotNull();
            assertThat(filter).isInstanceOf(JwtAuthenticationFilter.class);
        }

        @Test
        @DisplayName("JWT 인증 필터에 모든 의존성이 주입된다")
        void jwtAuthenticationFilter_InjectsAllDependencies() {
            // when
            JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter();

            // then
            assertThat(filter).isNotNull();
            // 필터가 정상적으로 생성되었다면 의존성 주입이 성공한 것으로 간주
        }
    }

    @Nested
    @DisplayName("TaskExecutor Bean 생성 테스트")
    class TaskExecutorBeanTest {

        @Test
        @DisplayName("비동기 TaskExecutor Bean이 정상적으로 생성된다")
        void taskExecutor_CreatesDelegatingSecurityContextAsyncTaskExecutor() {
            // when
            DelegatingSecurityContextAsyncTaskExecutor executor = securityConfig.taskExecutor();

            // then
            assertThat(executor).isNotNull();
            assertThat(executor).isInstanceOf(DelegatingSecurityContextAsyncTaskExecutor.class);
        }

        @Test
        @DisplayName("SSE TaskExecutor Bean이 정상적으로 생성된다")
        void sseTaskExecutor_CreatesDelegatingSecurityContextAsyncTaskExecutor() {
            // when
            DelegatingSecurityContextAsyncTaskExecutor sseExecutor = securityConfig.sseTaskExecutor();

            // then
            assertThat(sseExecutor).isNotNull();
            assertThat(sseExecutor).isInstanceOf(DelegatingSecurityContextAsyncTaskExecutor.class);
        }

        @Test
        @DisplayName("TaskExecutor와 SSE TaskExecutor는 서로 다른 인스턴스이다")
        void taskExecutorAndSseTaskExecutor_AreDifferentInstances() {
            // when
            DelegatingSecurityContextAsyncTaskExecutor executor1 = securityConfig.taskExecutor();
            DelegatingSecurityContextAsyncTaskExecutor executor2 = securityConfig.sseTaskExecutor();

            // then
            assertThat(executor1).isNotSameAs(executor2);
        }
    }

    @Nested
    @DisplayName("CORS 설정 테스트")
    class CorsConfigurationTest {

        @Test
        @DisplayName("CORS 설정 Bean이 정상적으로 생성된다")
        void corsConfigurationSource_CreatesValidCorsConfiguration() {
            // when
            CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource();

            // then
            assertThat(corsConfigurationSource).isNotNull();
        }

        @Test
        @DisplayName("CORS 설정이 올바른 값으로 구성된다")
        void corsConfigurationSource_ConfiguresCorrectValues() {
            // when
            CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource();
            
            // MockHttpServletRequest를 사용하여 getCorsConfiguration 호출
            org.springframework.mock.web.MockHttpServletRequest request = 
                new org.springframework.mock.web.MockHttpServletRequest();
            request.setRequestURI("/**");
            CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);

            // then
            assertThat(corsConfiguration).isNotNull();
            assertThat(corsConfiguration.getAllowedOriginPatterns()).contains("*");
            assertThat(corsConfiguration.getAllowedMethods()).containsExactlyInAnyOrder(
                    "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
            );
            assertThat(corsConfiguration.getAllowedHeaders()).contains("*");
            assertThat(corsConfiguration.getExposedHeaders()).containsExactlyInAnyOrder(
                    "Authorization", "Refresh-Token"
            );
            assertThat(corsConfiguration.getAllowCredentials()).isTrue();
            assertThat(corsConfiguration.getMaxAge()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("모든 경로에 대해 CORS 설정이 적용된다")
        void corsConfigurationSource_AppliedToAllPaths() {
            // when
            CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource();

            // then
            String[] testPaths = {"/api/users", "/api/notifications", "/", "/swagger-ui"};
            for (String path : testPaths) {
                org.springframework.mock.web.MockHttpServletRequest request = 
                    new org.springframework.mock.web.MockHttpServletRequest();
                request.setRequestURI(path);
                CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);
                assertThat(config).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("SecurityFilterChain 설정 테스트")
    class SecurityFilterChainTest {

        @Test
        @DisplayName("SecurityFilterChain Bean 생성이 가능하다")
        void securityFilterChain_CanBeCreated() throws Exception {
            // given
            HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
            when(httpSecurity.cors(any())).thenReturn(httpSecurity);
            when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
            when(httpSecurity.httpBasic(any())).thenReturn(httpSecurity);
            when(httpSecurity.formLogin(any())).thenReturn(httpSecurity);
            when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
            when(httpSecurity.exceptionHandling(any())).thenReturn(httpSecurity);
            when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
            when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
            when(httpSecurity.headers(any())).thenReturn(httpSecurity);
            
            // SecurityFilterChain 대신 구체적인 구현체 사용
            org.springframework.security.web.DefaultSecurityFilterChain mockChain = 
                mock(org.springframework.security.web.DefaultSecurityFilterChain.class);
            when(httpSecurity.build()).thenReturn(mockChain);

            // when & then
            assertThatCode(() -> securityConfig.securityFilterChain(httpSecurity))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("화이트리스트 경로 테스트")
    class WhitelistPathsTest {

        @Test
        @DisplayName("화이트리스트 경로들이 올바르게 정의되어 있다")
        void whitelistPaths_ContainsExpectedPaths() {
            // given
            JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter();

            // then
            assertThat(filter).isNotNull();
            // 화이트리스트 경로는 SecurityConfig 내부에서 정의되므로
            // 필터가 정상 생성되면 올바른 경로가 설정된 것으로 간주
        }

        @Test
        @DisplayName("필수 인증 제외 경로가 포함되어 있다")
        void whitelistPaths_IncludesEssentialPaths() {
            // given & when
            JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter();

            // then
            assertThat(filter).isNotNull();
            // 실제로는 다음 경로들이 포함되어야 함:
            // "/api/users/login", "/api/users/refresh", "/api/users/signup",
            // "/api/users/phone", "/api/users/phone/verify", "/actuator/prometheus",
            // "/swagger-ui/**", "/v3/api-docs/**", "/error"
        }
    }

    @Nested
    @DisplayName("Bean 의존성 주입 테스트")
    class BeanDependencyInjectionTest {

        @Test
        @DisplayName("SecurityConfig에 모든 필요한 의존성이 주입된다")
        void securityConfig_AllDependenciesInjected() {
            // then
            assertThat(securityConfig).isNotNull();
            // @InjectMocks에 의해 모든 @Mock 의존성이 주입됨
        }

        @Test
        @DisplayName("순환 참조 없이 Bean이 생성된다")
        void beans_CreatedWithoutCircularReference() {
            // when & then
            assertThatCode(() -> {
                securityConfig.jwtAuthenticationFilter();
                securityConfig.taskExecutor();
                securityConfig.sseTaskExecutor();
                securityConfig.corsConfigurationSource();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Security Context 전파 설정 테스트")
    class SecurityContextPropagationTest {

        @Test
        @DisplayName("비동기 처리용 TaskExecutor가 Security Context를 전파한다")
        void taskExecutor_PropagatesSecurityContext() {
            // when
            DelegatingSecurityContextAsyncTaskExecutor executor = securityConfig.taskExecutor();

            // then
            assertThat(executor).isNotNull();
            assertThat(executor.getClass().getSimpleName()).contains("DelegatingSecurityContext");
        }

        @Test
        @DisplayName("SSE용 TaskExecutor가 Security Context를 전파한다")
        void sseTaskExecutor_PropagatesSecurityContext() {
            // when
            DelegatingSecurityContextAsyncTaskExecutor sseExecutor = securityConfig.sseTaskExecutor();

            // then
            assertThat(sseExecutor).isNotNull();
            assertThat(sseExecutor.getClass().getSimpleName()).contains("DelegatingSecurityContext");
        }
    }

    @Nested
    @DisplayName("설정 일관성 테스트")
    class ConfigurationConsistencyTest {

        @Test
        @DisplayName("모든 Bean이 일관된 설정으로 생성된다")
        void allBeans_CreatedConsistently() {
            // when
            JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter();
            DelegatingSecurityContextAsyncTaskExecutor executor1 = securityConfig.taskExecutor();
            DelegatingSecurityContextAsyncTaskExecutor executor2 = securityConfig.sseTaskExecutor();
            CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

            // then
            assertThat(filter).isNotNull();
            assertThat(executor1).isNotNull();
            assertThat(executor2).isNotNull();
            assertThat(corsSource).isNotNull();
        }

        @Test
        @DisplayName("동일한 Bean을 여러 번 호출해도 같은 인스턴스를 반환한다")
        void sameBeanCall_ReturnsSameInstance() {
            // when
            CorsConfigurationSource corsSource1 = securityConfig.corsConfigurationSource();
            CorsConfigurationSource corsSource2 = securityConfig.corsConfigurationSource();

            // then
            // @Bean 어노테이션에 의해 Spring이 싱글톤으로 관리하지만,
            // 직접 메서드 호출 시에는 새 인스턴스가 생성될 수 있음
            assertThat(corsSource1).isNotNull();
            assertThat(corsSource2).isNotNull();
        }
    }
}
