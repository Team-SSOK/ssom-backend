package kr.ssok.ssom.backend.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM Issue 초안 작성 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "LLM Issue 초안 작성 응답")
public class LlmIssueResponseDto {
    
    @Schema(description = "원본 로그 데이터")
    private List<LogRequestDto> log;
    
    @Schema(description = "Issue 초안 메시지")
    private IssueMessageDto message;
    
    /**
     * Issue 초안 메시지 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Issue 초안 메시지")
    public static class IssueMessageDto {
        
        @Schema(description = "Issue 제목", example = "hotfix: Authorization 헤더 누락 시 인증 오류 발생")
        private String title;
        
        @Schema(description = "Issue 설명", example = "운영 환경에서 Authorization 헤더가 없거나 형식이 잘못된 요청에 대해...")
        private String description;
        
        @Schema(description = "오류 위치")
        private LocationDto location;
        
        @Schema(description = "원인 분석", example = "Authorization 헤더가 없거나 'Bearer ' 접두어가 없으면 인증 실패 처리로 바로 응답을 종료함")
        private String cause;
        
        @Schema(description = "재현 단계")
        private List<String> reproductionSteps;
        
        @Schema(description = "로그 메시지", example = "Authentication error: Authorization header is missing or invalid")
        private String log;
        
        @Schema(description = "해결 방안", example = "클라이언트 요청 시 Authorization 헤더가 반드시 포함되도록 요청 검증 강화")
        private String solution;
        
        @Schema(description = "참조 파일들", example = "JwtAuthenticationFilter.java, JwtAuthenticationEntryPoint.java")
        private String references;
    }
    
    /**
     * 오류 위치 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "오류 위치 정보")
    public static class LocationDto {
        
        @Schema(description = "파일명", example = "JwtAuthenticationFilter.java")
        private String file;
        
        @Schema(description = "함수명", example = "filter()")
        private String function;
    }
}
