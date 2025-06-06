package kr.ssok.ssom.backend.domain.user.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 인증 실패 시 호출되는 메서드 (Servlet 기반)
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        
        String requestUri = request.getRequestURI();
        log.error("Unauthorized error for request [{}]: {}", requestUri, authException.getMessage());
        
        // 응답이 이미 커밋된 경우 처리하지 않음 (SSE 등의 경우)
        if (response.isCommitted()) {
            log.warn("Response already committed for request [{}]. Cannot send error response.", requestUri);
            return;
        }
        
        // SSE 요청인 경우 특별 처리
        if (requestUri.contains("/subscribe") || "text/event-stream".equals(request.getHeader("Accept"))) {
            log.warn("SSE request authentication failed [{}]. Closing connection.", requestUri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        // 응답 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 에러 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("isSuccess", false);
        errorResponse.put("code", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("message", "인증에 실패하였습니다. 로그인이 필요합니다.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", requestUri);

        // JSON 응답 전송
        try {
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("Failed to send error response for request [{}]: {}", requestUri, e.getMessage());
        }
    }
}
