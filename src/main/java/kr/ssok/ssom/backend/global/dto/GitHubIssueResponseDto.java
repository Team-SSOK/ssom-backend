package kr.ssok.ssom.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GitHub Issue 생성 응답 DTO (GitHub API 응답)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GitHub Issue 생성 응답")
public class GitHubIssueResponseDto {
    
    @Schema(description = "GitHub Issue ID", example = "123456789")
    private Long id;
    
    @Schema(description = "GitHub Issue 번호", example = "123")
    private Long number;
    
    @Schema(description = "Issue 제목", example = "hotfix: Authorization 헤더 누락 시 인증 오류 발생")
    private String title;
    
    @Schema(description = "Issue 본문", example = "## 문제 설명...")
    private String body;
    
    @Schema(description = "Issue 상태", example = "open")
    private String state;
    
    @Schema(description = "GitHub Issue URL", example = "https://github.com/Team-SSOK/ssom-backend/issues/123")
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @Schema(description = "API URL", example = "https://api.github.com/repos/Team-SSOK/ssom-backend/issues/123")
    private String url;
    
    @Schema(description = "Issue 생성자")
    private GitHubUserDto user;
    
    @Schema(description = "담당자 목록")
    private List<GitHubUserDto> assignees;
    
    @Schema(description = "라벨 목록")
    private List<GitHubLabelDto> labels;
    
    @Schema(description = "생성 일시", example = "2025-05-30T10:00:00Z")
    @JsonProperty("created_at")
    private String createdAt;
    
    @Schema(description = "수정 일시", example = "2025-05-30T10:30:00Z")
    @JsonProperty("updated_at")
    private String updatedAt;
    
    /**
     * GitHub 사용자 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "GitHub 사용자 정보")
    public static class GitHubUserDto {
        
        @Schema(description = "GitHub 사용자 ID", example = "12345")
        private Long id;
        
        @Schema(description = "GitHub 사용자명", example = "github_user1")
        private String login;
        
        @Schema(description = "GitHub 사용자 URL", example = "https://github.com/github_user1")
        @JsonProperty("html_url")
        private String htmlUrl;
    }
    
    /**
     * GitHub 라벨 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "GitHub 라벨 정보")
    public static class GitHubLabelDto {
        
        @Schema(description = "라벨 ID", example = "123")
        private Long id;
        
        @Schema(description = "라벨명", example = "bug")
        private String name;
        
        @Schema(description = "라벨 색상", example = "d73a4a")
        private String color;
        
        @Schema(description = "라벨 설명", example = "Something isn't working")
        private String description;
    }
}
