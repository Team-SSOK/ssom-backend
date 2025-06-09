package kr.ssok.ssom.backend.domain.alert.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SSE 관련 예외를 처리하는 전용 Exception Handler
 * SSE 특성상 응답이 이미 커밋된 상태에서 발생하는 예외를 안전하게 처리
 */
@Slf4j
@RestControllerAdvice
public class SseExceptionHandler {

    /**
     * SSE 요청에서 발생하는 AuthorizationDeniedException 처리
     * 응답이 이미 커밋된 상태에서는 별도 처리 없이 로깅만 수행
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Void> handleSseAuthorizationDenied(
            AuthorizationDeniedException ex, 
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String requestUri = request.getRequestURI();
        
        // SSE 요청인지 확인
        boolean isSseRequest = requestUri.contains("/subscribe") || 
                              "text/event-stream".equals(request.getHeader("Accept")) ||
                              response.getContentType() != null && response.getContentType().contains("text/event-stream");
        
        if (isSseRequest) {
            // 응답이 이미 커밋된 경우
            if (response.isCommitted()) {
                log.warn("[SSE 예외 처리] 응답이 이미 커밋된 SSE 요청에서 AuthorizationDeniedException 발생. " +
                        "요청 URI: {}, 예외 메시지: {}", requestUri, ex.getMessage());
                return null; // 이미 커밋된 응답에는 추가 처리 불가
            } else {
                log.warn("[SSE 예외 처리] SSE 요청에서 AuthorizationDeniedException 발생. " +
                        "요청 URI: {}, 응답 상태를 401로 설정", requestUri);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        
        // 일반 요청인 경우 다른 핸들러에서 처리하도록 예외를 다시 던짐
        throw ex;
    }

    /**
     * SSE 관련 IOException 처리 (연결 끊김 등)
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Void> handleSseIOException(
            IOException ex, 
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String requestUri = request.getRequestURI();
        
        // SSE 요청인지 확인
        boolean isSseRequest = requestUri.contains("/subscribe") || 
                              "text/event-stream".equals(request.getHeader("Accept")) ||
                              response.getContentType() != null && response.getContentType().contains("text/event-stream");
        
        if (isSseRequest) {
            if (response.isCommitted()) {
                log.info("[SSE 예외 처리] SSE 연결에서 IOException 발생 (클라이언트 연결 끊김 가능성). " +
                        "요청 URI: {}, 예외 메시지: {}", requestUri, ex.getMessage());
                return null; // 이미 커밋된 응답에는 처리 불가
            } else {
                log.warn("[SSE 예외 처리] SSE 요청에서 IOException 발생. " +
                        "요청 URI: {}, 예외: {}", requestUri, ex.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        
        // SSE 요청이 아닌 경우 다른 핸들러에서 처리하도록 예외를 다시 던짐
        throw new RuntimeException(ex);
    }

    /**
     * SSE Emitter 관련 예외 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> handleSseIllegalStateException(
            IllegalStateException ex, 
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String requestUri = request.getRequestURI();
        
        // SSE 요청인지 확인
        boolean isSseRequest = requestUri.contains("/subscribe") || 
                              "text/event-stream".equals(request.getHeader("Accept")) ||
                              response.getContentType() != null && response.getContentType().contains("text/event-stream");
        
        if (isSseRequest) {
            if (response.isCommitted()) {
                log.warn("[SSE 예외 처리] SSE 요청에서 IllegalStateException 발생. " +
                        "요청 URI: {}, 예외 메시지: {}", requestUri, ex.getMessage());
                return null; // 이미 커밋된 응답에는 처리 불가
            } else {
                log.warn("[SSE 예외 처리] SSE 요청에서 IllegalStateException 발생. " +
                        "요청 URI: {}, 예외: {}", requestUri, ex.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        
        // SSE 요청이 아닌 경우 다른 핸들러에서 처리하도록 예외를 다시 던짐
        throw ex;
    }
}
