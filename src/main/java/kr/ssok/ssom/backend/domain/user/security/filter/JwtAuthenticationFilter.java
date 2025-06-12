package kr.ssok.ssom.backend.domain.user.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import kr.ssok.ssom.backend.global.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserService userService;
    private final List<String> whitelistPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String BLACKLIST_TOKEN_PREFIX = "blacklist:token:";

    /**
     * 생성자 - SecurityConfig에서 의존성 주입
     */
    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            RedisTemplate<String, String> redisTemplate,
            UserService userService,
            List<String> whitelistPaths) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.whitelistPaths = whitelistPaths;
    }

    /**
     * JWT 토큰 검증 및 인증 정보 설정을 위한 필터
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        boolean isSseRequest = requestPath.contains("/subscribe") || 
                              "text/event-stream".equals(request.getHeader("Accept"));

        // 인증이 필요없는 요청은 토큰 검증 없이 통과
        if (isWhiteListPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 인증 헤더 확인
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Authorization header is missing or invalid for path: {}", requestPath);
                
                // SSE 요청의 경우 특별 처리
                if (isSseRequest) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                sendErrorResponse(response, "Authorization header is missing or invalid", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 토큰 추출 및 검증
            String token = jwtTokenProvider.resolveToken(authHeader);
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid JWT token for path: {}", requestPath);
                
                // SSE 요청의 경우 특별 처리
                if (isSseRequest) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                sendErrorResponse(response, "Invalid JWT token", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // SSE 토큰인 경우 블랙리스트 확인 생략 (장기 토큰이므로)
            boolean isSseToken = jwtTokenProvider.isSseToken(token);
            
            // 일반 토큰인 경우에만 블랙리스트 확인
            if (!isSseToken) {
                String blacklistKey = BLACKLIST_TOKEN_PREFIX + token;
                Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    log.warn("Blacklisted token used for path: {}", requestPath);
                    
                    // SSE 요청의 경우 특별 처리
                    if (isSseRequest) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    sendErrorResponse(response, "Token is blacklisted", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }

            // 토큰에서 사용자 ID 추출
            String employeeId = jwtTokenProvider.getUserIdFromToken(token);
            if (employeeId == null) {
                log.warn("Could not extract user ID from token for path: {}", requestPath);
                
                // SSE 요청의 경우 특별 처리
                if (isSseRequest) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                sendErrorResponse(response, "Could not extract user ID from token", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 사용자 정보 조회하여 UserPrincipal 생성
            try {
                User user = userService.findUserByEmployeeId(employeeId);
                UserPrincipal userPrincipal = UserPrincipal.from(user);

                // Spring Security의 인증 정보 설정
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Successfully authenticated user: {} for path: {}", employeeId, requestPath);

            } catch (BaseException e) {
                log.warn("User not found for employeeId: {} in path: {}", employeeId, requestPath);
                
                // SSE 요청의 경우 특별 처리
                if (isSseRequest) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                sendErrorResponse(response, "User not found", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

        } catch (Exception e) {
            log.error("Authentication error for path: {}, error: {}", requestPath, e.getMessage());
            
            // SSE 요청의 경우 특별 처리
            if (isSseRequest) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            
            sendErrorResponse(response, "Authentication failed", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 인증이 필요 없는 경로인지 확인
     */
    private boolean isWhiteListPath(String requestPath) {
        return whitelistPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    /**
     * 인증 오류 응답 전송
     */
    private void sendErrorResponse(HttpServletResponse response, String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        String errorResponse = String.format(
                "{\"isSuccess\": false, \"code\": %d, \"message\": \"%s\"}",
                status, message
        );

        response.getWriter().write(errorResponse);
        response.getWriter().flush();
    }
}