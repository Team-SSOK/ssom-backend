package kr.ssok.ssom.backend.domain.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.security.filter.JwtAuthenticationFilter;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private UserService userService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final List<String> WHITELIST_PATHS = List.of(
            "/api/users/login",
            "/api/users/refresh",
            "/api/users/signup",
            "/actuator/health",
            "/swagger-ui/**"
    );

    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String TEST_BEARER_TOKEN = "Bearer " + TEST_TOKEN;
    private static final String TEST_USER_ID = "CHN0001";

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtTokenProvider, redisTemplate, userService, WHITELIST_PATHS);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }

    // Reflection을 사용하여 protected 메서드 테스트
    private void callDoFilterInternal() throws Exception {
        Method doFilterInternal = JwtAuthenticationFilter.class
                .getDeclaredMethod("doFilterInternal", HttpServletRequest.class,
                        HttpServletResponse.class, FilterChain.class);
        doFilterInternal.setAccessible(true);
        doFilterInternal.invoke(jwtAuthenticationFilter, request, response, filterChain);
    }

    @Nested
    @DisplayName("화이트리스트 경로 처리 테스트")
    class WhitelistPathTest {

        @Test
        @DisplayName("화이트리스트 경로는 인증 없이 통과한다")
        void doFilterInternal_WhitelistPath_PassesWithoutAuthentication() throws Exception {
            // given
            request.setRequestURI("/api/users/login");

            // when
            callDoFilterInternal();

            // then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenProvider, redisTemplate, userService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("와일드카드 패턴 화이트리스트 경로도 통과한다")
        void doFilterInternal_WildcardWhitelistPath_PassesWithoutAuthentication() throws Exception {
            // given
            request.setRequestURI("/swagger-ui/index.html");

            // when
            callDoFilterInternal();

            // then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenProvider, redisTemplate, userService);
        }

        @Test
        @DisplayName("화이트리스트에 없는 경로는 인증이 필요하다")
        void doFilterInternal_NonWhitelistPath_RequiresAuthentication() throws Exception {
            // given
            request.setRequestURI("/api/protected/resource");

            // when
            callDoFilterInternal();

            // then
            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("인증 헤더 검증 테스트")
    class AuthenticationHeaderTest {

        @Test
        @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
        void doFilterInternal_MissingAuthHeader_Returns401() throws Exception {
            // given
            request.setRequestURI("/api/protected/resource");

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer로 시작하지 않는 Authorization 헤더는 401을 반환한다")
        void doFilterInternal_InvalidAuthHeaderFormat_Returns401() throws Exception {
            // given
            request.setRequestURI("/api/protected/resource");
            request.addHeader("Authorization", "Basic sometoken");

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("유효한 Bearer 토큰 헤더는 처리된다")
        void doFilterInternal_ValidBearerHeader_ProcessesToken() throws Exception {
            // given
            request.setRequestURI("/api/protected/resource");
            request.addHeader("Authorization", TEST_BEARER_TOKEN);

            when(jwtTokenProvider.resolveToken(TEST_BEARER_TOKEN)).thenReturn(TEST_TOKEN);
            when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(false); // 토큰 검증 실패로 설정

            // when
            callDoFilterInternal();

            // then
            verify(jwtTokenProvider).resolveToken(TEST_BEARER_TOKEN);
            verify(jwtTokenProvider).validateToken(TEST_TOKEN);
        }
    }

    @Nested
    @DisplayName("JWT 토큰 검증 테스트")
    class JwtTokenValidationTest {

        @BeforeEach
        void setUpValidRequest() {
            request.setRequestURI("/api/protected/resource");
            request.addHeader("Authorization", TEST_BEARER_TOKEN);
        }

        @Test
        @DisplayName("유효하지 않은 토큰은 401을 반환한다")
        void doFilterInternal_InvalidToken_Returns401() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(TEST_BEARER_TOKEN)).thenReturn(TEST_TOKEN);
            when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(false);

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("토큰 해결 실패 시 401을 반환한다")
        void doFilterInternal_TokenResolveFailed_Returns401() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(TEST_BEARER_TOKEN)).thenReturn(null);

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
            verify(jwtTokenProvider, never()).validateToken(any());
        }

        @Test
        @DisplayName("유효한 토큰은 다음 단계로 진행한다")
        void doFilterInternal_ValidToken_ProceedsToNextStep() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(TEST_BEARER_TOKEN)).thenReturn(TEST_TOKEN);
            when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
            when(redisTemplate.hasKey("blacklist:token:" + TEST_TOKEN)).thenReturn(false);
            when(jwtTokenProvider.getUserIdFromToken(TEST_TOKEN)).thenReturn(null); // 다음 단계에서 실패하도록

            // when
            callDoFilterInternal();

            // then
            verify(jwtTokenProvider).validateToken(TEST_TOKEN);
            verify(redisTemplate).hasKey("blacklist:token:" + TEST_TOKEN);
        }
    }

    @Nested
    @DisplayName("블랙리스트 토큰 검증 테스트")
    class BlacklistTokenTest {

        @BeforeEach
        void setUpValidToken() {
            request.setRequestURI("/api/protected/resource");
            request.addHeader("Authorization", TEST_BEARER_TOKEN);
            when(jwtTokenProvider.resolveToken(TEST_BEARER_TOKEN)).thenReturn(TEST_TOKEN);
            when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
        }

        @Test
        @DisplayName("블랙리스트에 있는 토큰은 401을 반환한다")
        void doFilterInternal_BlacklistedToken_Returns401() throws Exception {
            // given
            when(redisTemplate.hasKey("blacklist:token:" + TEST_TOKEN)).thenReturn(true);

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
            verify(jwtTokenProvider, never()).getUserIdFromToken(any());
        }

        @Test
        @DisplayName("블랙리스트에 없는 토큰은 사용자 ID 추출 단계로 진행한다")
        void doFilterInternal_NonBlacklistedToken_ProceedsToUserIdExtraction() throws Exception {
            // given
            when(redisTemplate.hasKey("blacklist:token:" + TEST_TOKEN)).thenReturn(false);
            when(jwtTokenProvider.getUserIdFromToken(TEST_TOKEN)).thenReturn(null); // 다음 단계에서 실패하도록

            // when
            callDoFilterInternal();

            // then
            verify(jwtTokenProvider).getUserIdFromToken(TEST_TOKEN);
        }

        @Test
        @DisplayName("Redis 조회 실패 시 인증 실패로 처리한다")
        void doFilterInternal_RedisFailure_ReturnsUnauthorized() throws Exception {
            // given
            when(redisTemplate.hasKey("blacklist:token:" + TEST_TOKEN)).thenThrow(new RuntimeException("Redis error"));

            // when
            callDoFilterInternal();

            // then
            // Redis 에러 발생 시 catch 블록에서 인증 실패 처리
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
            // Redis 에러로 인해 getUserIdFromToken은 호출되지 않음
            verify(jwtTokenProvider, never()).getUserIdFromToken(any());
        }
    }

    @Nested
    @DisplayName("사용자 ID 추출 및 인증 설정 테스트")
    class UserIdExtractionAndAuthenticationTest {

        @BeforeEach
        void setUpValidTokenFlow() {
            request.setRequestURI("/api/protected/resource");
            request.addHeader("Authorization", TEST_BEARER_TOKEN);
            when(jwtTokenProvider.resolveToken(TEST_BEARER_TOKEN)).thenReturn(TEST_TOKEN);
            when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
            when(redisTemplate.hasKey("blacklist:token:" + TEST_TOKEN)).thenReturn(false);
        }

        @Test
        @DisplayName("사용자 ID 추출 실패 시 401을 반환한다")
        void doFilterInternal_UserIdExtractionFailed_Returns401() throws Exception {
            // given
            when(jwtTokenProvider.getUserIdFromToken(TEST_TOKEN)).thenReturn(null);

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("사용자 조회 실패 시 401을 반환한다")
        void doFilterInternal_UserNotFound_Returns401() throws Exception {
            // given
            when(jwtTokenProvider.getUserIdFromToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
            when(userService.findUserByEmployeeId(TEST_USER_ID))
                    .thenThrow(new BaseException(BaseResponseStatus.USER_NOT_FOUND));

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("유효한 사용자 정보로 인증이 성공한다")
        void doFilterInternal_ValidUser_AuthenticationSuccess() throws Exception {
            // given
            User user = User.builder()
                    .id(TEST_USER_ID)
                    .username("테스트사용자")
                    .password("password")
                    .phoneNumber("010-1234-5678")
                    .department(Department.CHANNEL)
                    .githubId("testuser")
                    .build();

            when(jwtTokenProvider.getUserIdFromToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
            when(userService.findUserByEmployeeId(TEST_USER_ID)).thenReturn(user);

            // when
            callDoFilterInternal();

            // then
            verify(filterChain).doFilter(request, response);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            assertThat(principal.getEmployeeId()).isEqualTo(TEST_USER_ID);
            assertThat(principal.getUsername()).isEqualTo("테스트사용자");
        }

        @Test
        @DisplayName("인증 성공 시 SecurityContext가 올바르게 설정된다")
        void doFilterInternal_AuthenticationSuccess_SecurityContextSetCorrectly() throws Exception {
            // given
            User user = User.builder()
                    .id(TEST_USER_ID)
                    .username("김철수")
                    .password("password")
                    .phoneNumber("010-9876-5432")
                    .department(Department.CORE_BANK)
                    .githubId("kimcs")
                    .build();

            when(jwtTokenProvider.getUserIdFromToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
            when(userService.findUserByEmployeeId(TEST_USER_ID)).thenReturn(user);

            // when
            callDoFilterInternal();

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();
            assertThat(authentication.getAuthorities()).isEmpty();

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            assertThat(principal.getDepartment()).isEqualTo(Department.CORE_BANK);
            assertThat(principal.getDepartmentCode()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("SSE 요청 특별 처리 테스트")
    class SseRequestHandlingTest {

        @Test
        @DisplayName("SSE 요청 경로에서 인증 실패 시 상태코드만 설정한다")
        void doFilterInternal_SseRequestAuthFailed_OnlySetStatusCode() throws Exception {
            // given
            request.setRequestURI("/api/notifications/subscribe");

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            assertThat(response.getContentType()).isNull(); // JSON 응답이 아님
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Accept 헤더가 text/event-stream인 요청은 SSE로 처리한다")
        void doFilterInternal_EventStreamAcceptHeader_TreatedAsSse() throws Exception {
            // given
            request.setRequestURI("/api/some/endpoint");
            request.addHeader("Accept", "text/event-stream");

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            assertThat(response.getContentType()).isNull();
        }

        @Test
        @DisplayName("일반 요청에서 인증 실패 시 JSON 응답을 반환한다")
        void doFilterInternal_RegularRequestAuthFailed_ReturnsJsonResponse() throws Exception {
            // given
            request.setRequestURI("/api/regular/endpoint");

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @BeforeEach
        void setUpForException() {
            request.setRequestURI("/api/protected/resource");
            request.addHeader("Authorization", TEST_BEARER_TOKEN);
        }

        @Test
        @DisplayName("JWT 처리 중 예외 발생 시 401을 반환한다")
        void doFilterInternal_JwtProcessingException_Returns401() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(TEST_BEARER_TOKEN))
                    .thenThrow(new RuntimeException("JWT processing error"));

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("UserService 예외 발생 시 401을 반환한다")
        void doFilterInternal_UserServiceException_Returns401() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(TEST_BEARER_TOKEN)).thenReturn(TEST_TOKEN);
            when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(jwtTokenProvider.getUserIdFromToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
            when(userService.findUserByEmployeeId(TEST_USER_ID))
                    .thenThrow(new RuntimeException("Database error"));

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("일반 예외 발생 시에도 적절히 처리한다")
        void doFilterInternal_GeneralException_HandledProperly() throws Exception {
            // given
            when(jwtTokenProvider.resolveToken(any())).thenThrow(new NullPointerException("Unexpected error"));

            // when
            callDoFilterInternal();

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }
    }
}
