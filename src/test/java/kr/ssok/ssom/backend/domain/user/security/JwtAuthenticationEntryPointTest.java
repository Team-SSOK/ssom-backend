package kr.ssok.ssom.backend.domain.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ssok.ssom.backend.domain.user.security.handler.JwtAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationEntryPoint 테스트")
class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtAuthenticationEntryPoint = new JwtAuthenticationEntryPoint();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("일반 요청 인증 실패 처리 테스트")
    class RegularRequestAuthenticationFailureTest {

        @Test
        @DisplayName("인증 실패 시 401 상태코드를 설정한다")
        void commence_AuthenticationFailed_Sets401Status() throws IOException {
            // given
            request.setRequestURI("/api/protected/resource");
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.getMessage()).thenReturn("Authentication failed");

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("인증 실패 시 올바른 Content-Type을 설정한다")
        void commence_AuthenticationFailed_SetsCorrectContentType() throws IOException {
            // given
            request.setRequestURI("/api/test");
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.getMessage()).thenReturn("Authentication failed");

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            // MockHttpServletResponse는 charset을 포함한 전체 content-type을 반환
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        }

        @Test
        @DisplayName("인증 실패 시 올바른 JSON 응답을 생성한다")
        void commence_AuthenticationFailed_CreatesCorrectJsonResponse() throws IOException {
            // given
            request.setRequestURI("/api/test");
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.getMessage()).thenReturn("Authentication failed");

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            String responseContent = response.getContentAsString();
            assertThat(responseContent).isNotEmpty();

            // JSON 파싱하여 검증
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);

            assertThat(responseMap.get("isSuccess")).isEqualTo(false);
            assertThat(responseMap.get("code")).isEqualTo(401);
            assertThat(responseMap.get("message")).isEqualTo("인증에 실패하였습니다. 로그인이 필요합니다.");
            assertThat(responseMap.get("path")).isEqualTo("/api/test");
            assertThat(responseMap.get("timestamp")).isNotNull();
        }

        @Test
        @DisplayName("다양한 URI에 대해 올바른 path를 응답에 포함한다")
        void commence_VariousUris_IncludesCorrectPathInResponse() throws IOException {
            // Test data
            String[] testUris = {
                    "/api/users/profile",
                    "/api/notifications/list",
                    "/api/admin/dashboard",
                    "/api/reports/generate"
            };

            for (String uri : testUris) {
                // given
                MockHttpServletRequest testRequest = new MockHttpServletRequest();
                MockHttpServletResponse testResponse = new MockHttpServletResponse();
                AuthenticationException authException = mock(AuthenticationException.class);
                when(authException.getMessage()).thenReturn("Authentication failed");

                testRequest.setRequestURI(uri);

                // when
                jwtAuthenticationEntryPoint.commence(testRequest, testResponse, authException);

                // then
                String responseContent = testResponse.getContentAsString();
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);

                assertThat(responseMap.get("path")).isEqualTo(uri);
            }
        }
    }

    @Nested
    @DisplayName("SSE 요청 인증 실패 처리 테스트")
    class SseRequestAuthenticationFailureTest {

        @Test
        @DisplayName("subscribe 경로 SSE 요청 시 상태코드만 설정한다")
        void commence_SseSubscribePath_OnlySetStatusCode() throws IOException {
            // given
            request.setRequestURI("/api/notifications/subscribe");
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.getMessage()).thenReturn("Authentication failed");

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).isEmpty(); // JSON 응답 없음
        }

        @Test
        @DisplayName("text/event-stream Accept 헤더 요청 시 상태코드만 설정한다")
        void commence_EventStreamAcceptHeader_OnlySetStatusCode() throws IOException {
            // given
            request.setRequestURI("/api/some/endpoint");
            request.addHeader("Accept", "text/event-stream");
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.getMessage()).thenReturn("Authentication failed");

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).isEmpty();
        }

        @Test
        @DisplayName("subscribe가 포함된 다양한 경로에서 SSE로 처리한다")
        void commence_VariousSubscribePaths_TreatedAsSse() throws IOException {
            // Test data
            String[] ssePaths = {
                    "/api/notifications/subscribe",
                    "/api/alerts/subscribe/user",
                    "/subscribe/events",
                    "/api/live/subscribe"
            };

            for (String path : ssePaths) {
                // given
                MockHttpServletRequest testRequest = new MockHttpServletRequest();
                MockHttpServletResponse testResponse = new MockHttpServletResponse();
                AuthenticationException authException = mock(AuthenticationException.class);
                when(authException.getMessage()).thenReturn("Authentication failed");

                testRequest.setRequestURI(path);

                // when
                jwtAuthenticationEntryPoint.commence(testRequest, testResponse, authException);

                // then
                assertThat(testResponse.getStatus()).isEqualTo(401);
                assertThat(testResponse.getContentAsString()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("응답 커밋 상태 처리 테스트")
    class ResponseCommittedTest {

        @Test
        @DisplayName("응답이 커밋되지 않은 경우 정상 처리한다")
        void commence_ResponseNotCommitted_ProcessesNormally() throws IOException {
            // given
            request.setRequestURI("/api/test");
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.getMessage()).thenReturn("Authentication failed");

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("null AuthenticationException도 처리한다")
        void commence_NullAuthenticationException_HandledGracefully() throws IOException {
            // given
            request.setRequestURI("/api/test");
            // null AuthenticationException 테스트이므로 mock 설정 불필요

            // when
            jwtAuthenticationEntryPoint.commence(request, response, null);

            // then
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).isNotEmpty();
        }

        @Test
        @DisplayName("빈 메시지의 AuthenticationException도 처리한다")
        void commence_EmptyMessageAuthenticationException_HandledGracefully() throws IOException {
            // given
            request.setRequestURI("/api/test");
            // 이 테스트에서만 mock 설정
            AuthenticationException emptyMessageException = mock(AuthenticationException.class);
            when(emptyMessageException.getMessage()).thenReturn("");

            // when
            jwtAuthenticationEntryPoint.commence(request, response, emptyMessageException);

            // then
            assertThat(response.getStatus()).isEqualTo(401);
            String responseContent = response.getContentAsString();
            assertThat(responseContent).isNotEmpty();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
            assertThat(responseMap.get("message")).isEqualTo("인증에 실패하였습니다. 로그인이 필요합니다.");
        }
    }

    @Nested
    @DisplayName("타임스탬프 검증 테스트")
    class TimestampValidationTest {

        @Test
        @DisplayName("응답에 현재 시간과 근사한 타임스탬프가 포함된다")
        void commence_IncludesReasonableTimestamp() throws IOException {
            // given
            request.setRequestURI("/api/test");
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.getMessage()).thenReturn("Authentication failed");
            long beforeTime = System.currentTimeMillis();

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            long afterTime = System.currentTimeMillis();
            String responseContent = response.getContentAsString();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);

            Long timestamp = ((Number) responseMap.get("timestamp")).longValue();
            assertThat(timestamp).isBetween(beforeTime, afterTime);
        }
    }

    @Nested
    @DisplayName("메시지 다국화 대응 테스트")
    class MessageLocalizationTest {

        @Test
        @DisplayName("한글 메시지가 올바르게 인코딩된다")
        void commence_KoreanMessage_ProperlyEncoded() throws IOException {
            // given
            request.setRequestURI("/api/test");
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.getMessage()).thenReturn("Authentication failed");

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            String responseContent = response.getContentAsString();
            assertThat(responseContent).contains("인증에 실패하였습니다");
            assertThat(responseContent).contains("로그인이 필요합니다");

            // JSON으로 파싱이 가능한지 확인 (인코딩 이슈 없음)
            ObjectMapper objectMapper = new ObjectMapper();
            assertThatCode(() -> objectMapper.readValue(responseContent, Map.class))
                    .doesNotThrowAnyException();
        }
    }
}