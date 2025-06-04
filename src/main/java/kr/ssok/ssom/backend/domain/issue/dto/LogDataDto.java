package kr.ssok.ssom.backend.domain.issue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개별 로그 데이터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "개별 로그 데이터")
public class LogDataDto {
    
    @Schema(description = "로그 레벨", example = "ERROR")
    private String level;
    
    @Schema(description = "로거 클래스", example = "kr.ssok.gateway.security.filter.JwtAuthenticationFilter")
    private String logger;
    
    @Schema(description = "스레드명", example = "reactor-http-epoll-1")
    private String thread;
    
    @Schema(description = "로그 메시지", example = "Authentication error: Authorization header is missing or invalid")
    private String message;
    
    @Schema(description = "애플리케이션명", example = "ssok-gateway-service")
    private String app;
    
    @Schema(description = "타임스탬프", example = "2025-05-30T10:00:00.000Z")
    private String timestamp;
    
    @Schema(description = "로그 ID", example = "log_001")
    private String logId;
}
